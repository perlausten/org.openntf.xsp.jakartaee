/**
 * Copyright (c) 2018-2023 Contributors to the XPages Jakarta EE Support Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openntf.xsp.microprofile.metrics.exporter;

import java.io.StringWriter;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.eclipse.microprofile.config.Config;
import org.eclipse.microprofile.config.ConfigProvider;
import org.eclipse.microprofile.metrics.ConcurrentGauge;
import org.eclipse.microprofile.metrics.Counter;
import org.eclipse.microprofile.metrics.Gauge;
import org.eclipse.microprofile.metrics.Histogram;
import org.eclipse.microprofile.metrics.Metadata;
import org.eclipse.microprofile.metrics.Metered;
import org.eclipse.microprofile.metrics.Metric;
import org.eclipse.microprofile.metrics.MetricID;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.eclipse.microprofile.metrics.SimpleTimer;
import org.eclipse.microprofile.metrics.Snapshot;
import org.eclipse.microprofile.metrics.Timer;
import org.openntf.xsp.microprofile.metrics.config.MetricsAppConfigSource;

import io.smallrye.metrics.MetricRegistries;
import io.smallrye.metrics.TagsUtils;
import io.smallrye.metrics.exporters.ExporterUtil;
import io.smallrye.metrics.exporters.JsonExporter;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
import jakarta.json.JsonValue;
import jakarta.json.JsonWriter;
import jakarta.json.spi.JsonProvider;
import jakarta.json.stream.JsonGenerator;

/**
 * This variant of {@link JsonExporter} will query the current application for a
 * context path and, when present, filter metrics to only those with either a
 * matching {@value MetricsAppConfigSource#TAG_APP} tag or no such tag.
 * 
 * @author hrupp
 * @author Jesse Gallagher
 * @since 2.10.0
 */
public class FilteringJsonExporter extends AbstractFilteringExporter {

	private final Map<String, String> globalTags;
	private final JsonProvider json;

	public FilteringJsonExporter(JsonProvider json, String appName) {
		super(appName);
		this.json = json;

		Map<String, String> tags;
		try {
			Config config = ConfigProvider.getConfig();
			tags = TagsUtils.parseGlobalTags(config.getOptionalValue("mp.metrics.tags", String.class).orElse("")); //$NON-NLS-1$ //$NON-NLS-2$
		} catch (IllegalStateException | ExceptionInInitializerError | NoClassDefFoundError t) {
			// MP Config implementation is probably not available
			tags = Collections.emptyMap();
		}
		this.globalTags = tags;
	}

	@Override
	public StringBuilder exportOneScope(MetricRegistry.Type scope) {
		return stringify(exportOneRegistry(MetricRegistries.get(scope)));
	}

	@Override
	public StringBuilder exportAllScopes() {

		JsonObjectBuilder root = json.createObjectBuilder();

		root.add("base", exportOneRegistry(MetricRegistries.get(MetricRegistry.Type.BASE))); //$NON-NLS-1$
		root.add("vendor", exportOneRegistry(MetricRegistries.get(MetricRegistry.Type.VENDOR))); //$NON-NLS-1$
		root.add("application", exportOneRegistry(MetricRegistries.get(MetricRegistry.Type.APPLICATION))); //$NON-NLS-1$

		return stringify(root.build());
	}

	@Override
	public StringBuilder exportOneMetric(MetricRegistry.Type scope, MetricID metricID) {
		MetricRegistry registry = MetricRegistries.get(scope);
		Map<MetricID, Metric> metricMap = registry.getMetrics()
				.entrySet()
				.stream()
				.filter(e -> matchesApp(e.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		Map<String, Metadata> metadataMap = registry.getMetadata();

		Metric m = metricMap.get(metricID);

		Map<MetricID, Metric> outMap = new HashMap<>(1);
		outMap.put(metricID, m);

		JsonObjectBuilder root = json.createObjectBuilder();
		exportMetricsForMap(outMap, metadataMap).forEach(root::add);
		return stringify(root.build());
	}

	@Override
	public StringBuilder exportMetricsByName(MetricRegistry.Type scope, String name) {
		MetricRegistry registry = MetricRegistries.get(scope);
		Map<MetricID, Metric> metricMap = registry.getMetrics()
				.entrySet()
				.stream()
				.filter(e -> e.getKey().getName().equals(name)).filter(e -> matchesApp(e.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		Map<String, Metadata> metadataMap = registry.getMetadata();

		JsonObjectBuilder root = json.createObjectBuilder();
		exportMetricsForMap(metricMap, metadataMap).forEach(root::add);
		return stringify(root.build());
	}

	@Override
	public String getContentType() {
		return "application/json"; //$NON-NLS-1$
	}

	private static final Map<String, ?> JSON_CONFIG = Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true);

	StringBuilder stringify(JsonObject obj) {
		StringWriter out = new StringWriter();
		try (JsonWriter writer = json.createWriterFactory(JSON_CONFIG).createWriter(out)) {
			writer.writeObject(obj);
		}
		return new StringBuilder(out.toString());
	}

	private Map<String, JsonValue> exportMetricsByName(Map<MetricID, Metric> metricMap, Metadata metadata) {
		Map<String, JsonValue> result = new HashMap<>();
		JsonObjectBuilder builder = json.createObjectBuilder();

		switch (metadata.getTypeRaw()) {
			case GAUGE:
			case COUNTER:
				metricMap.forEach((metricID, metric) -> {
					result.put(metricID.getName() + createTagsString(metricID.getTags()),
							exportSimpleMetric(metricID, metric));
				});
				break;
			case METERED:
				metricMap.forEach((metricID, value) -> {
					Metered metric = (Metered) value;
					meterValues(metric, createTagsString(metricID.getTags())).forEach(builder::add);
				});
				result.put(metadata.getName(), builder.build());
				break;
			case CONCURRENT_GAUGE:
				metricMap.forEach((metricID, value) -> {
					ConcurrentGauge metric = (ConcurrentGauge) value;
					exportConcurrentGauge(metric, createTagsString(metricID.getTags())).forEach(builder::add);
				});
				result.put(metadata.getName(), builder.build());
				break;
			case SIMPLE_TIMER:
				metricMap.forEach((metricID, value) -> {
					SimpleTimer metric = (SimpleTimer) value;
					exportSimpleTimer(metric, metadata.getUnit(), createTagsString(metricID.getTags()))
							.forEach(builder::add);
				});
				result.put(metadata.getName(), builder.build());
				break;
			case TIMER:
				metricMap.forEach((metricID, value) -> {
					Timer metric = (Timer) value;
					exportTimer(metric, metadata.getUnit(), createTagsString(metricID.getTags())).forEach(builder::add);
				});
				result.put(metadata.getName(), builder.build());
				break;
			case HISTOGRAM:
				metricMap.forEach((metricID, value) -> {
					Histogram metric = (Histogram) value;
					exportHistogram(metric, createTagsString(metricID.getTags())).forEach(builder::add);
				});
				result.put(metadata.getName(), builder.build());
				break;
			default:
				throw new IllegalArgumentException("Not supported: " + metadata.getTypeRaw());
		}
		return result;
	}

	private JsonObject exportOneRegistry(MetricRegistry registry) {
		Map<MetricID, Metric> metricMap = registry.getMetrics().entrySet().stream().filter(e -> matchesApp(e.getKey()))
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
		Map<String, Metadata> metadataMap = registry.getMetadata();

		JsonObjectBuilder root = json.createObjectBuilder();
		exportMetricsForMap(metricMap, metadataMap).forEach(root::add);
		return root.build();
	}

	private Map<String, JsonValue> exportMetricsForMap(Map<MetricID, Metric> metricMap,
			Map<String, Metadata> metadataMap) {
		Map<String, JsonValue> result = new HashMap<>();

		// split into groups by metric name
		Map<String, Map<MetricID, Metric>> metricsGroupedByName = metricMap.entrySet().stream()
				.collect(Collectors.groupingBy(entry -> entry.getKey().getName(),
						Collectors.mapping(e -> e, Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))));
		// and then for each group, perform the export
		metricsGroupedByName.entrySet().stream()
				.map(entry -> exportMetricsByName(entry.getValue(), metadataMap.get(entry.getKey()))).forEach(map -> {
					map.forEach(result::put);
				});
		return result;
	}

	private JsonValue exportSimpleMetric(MetricID metricID, Metric metric) {
		Number val = getValueFromMetric(metric, metricID.getName());
		if (val instanceof Double) {
			return json.createValue((Double) val);
		} else if (val instanceof Float) {
			return json.createValue((Float) val);
		} else if (val instanceof Integer) {
			return json.createValue((Integer) val);
		} else if (val instanceof Long) {
			return json.createValue((Long) val);
		} else {
			throw new IllegalStateException();
		}
	}

	private Map<String, JsonValue> meterValues(Metered meter, String tags) {
		Map<String, JsonValue> map = new HashMap<>();
		map.put("count" + tags, json.createValue(meter.getCount())); //$NON-NLS-1$
		map.put("meanRate" + tags, json.createValue(meter.getMeanRate())); //$NON-NLS-1$
		map.put("oneMinRate" + tags, json.createValue(meter.getOneMinuteRate())); //$NON-NLS-1$
		map.put("fiveMinRate" + tags, json.createValue(meter.getFiveMinuteRate())); //$NON-NLS-1$
		map.put("fifteenMinRate" + tags, json.createValue(meter.getFifteenMinuteRate())); //$NON-NLS-1$
		return map;
	}

	private Map<String, JsonValue> exportConcurrentGauge(ConcurrentGauge concurrentGauge, String tags) {
		Map<String, JsonValue> map = new HashMap<>();
		map.put("current" + tags, json.createValue(concurrentGauge.getCount())); //$NON-NLS-1$
		map.put("max" + tags, json.createValue(concurrentGauge.getMax())); //$NON-NLS-1$
		map.put("min" + tags, json.createValue(concurrentGauge.getMin())); //$NON-NLS-1$
		return map;
	}

	private JsonObject exportSimpleTimer(SimpleTimer timer, String unit, String tags) {
		JsonObjectBuilder builder = json.createObjectBuilder();
		builder.add("count" + tags, timer.getCount()); //$NON-NLS-1$
		builder.add("elapsedTime" + tags, toBase(timer.getElapsedTime().toNanos(), unit)); //$NON-NLS-1$
		Duration minTimeDuration = timer.getMinTimeDuration();
		if (minTimeDuration != null) {
			builder.add("minTimeDuration" + tags, toBase(minTimeDuration.toNanos(), unit)); //$NON-NLS-1$
		} else {
			builder.add("minTimeDuration" + tags, JsonValue.NULL); //$NON-NLS-1$
		}
		Duration maxTimeDuration = timer.getMaxTimeDuration();
		if (maxTimeDuration != null) {
			builder.add("maxTimeDuration" + tags, toBase(maxTimeDuration.toNanos(), unit)); //$NON-NLS-1$
		} else {
			builder.add("maxTimeDuration" + tags, JsonValue.NULL); //$NON-NLS-1$
		}
		return builder.build();
	}

	private JsonObject exportTimer(Timer timer, String unit, String tags) {
		JsonObjectBuilder builder = json.createObjectBuilder();
		snapshotValues(timer.getSnapshot(), unit, tags).forEach(builder::add);
		meterValues(timer, tags).forEach(builder::add);
		builder.add("elapsedTime" + tags, toBase(timer.getElapsedTime().toNanos(), unit)); //$NON-NLS-1$
		return builder.build();
	}

	private Map<String, JsonValue> exportHistogram(Histogram histogram, String tags) {
		Map<String, JsonValue> map = new HashMap<>();
		map.put("count" + tags, json.createValue(histogram.getCount())); //$NON-NLS-1$
		map.put("sum" + tags, json.createValue(histogram.getSum())); //$NON-NLS-1$
		snapshotValues(histogram.getSnapshot(), tags).forEach((map::put));
		return map;
	}

	private Map<String, JsonValue> snapshotValues(Snapshot snapshot, String tags) {
		Map<String, JsonValue> map = new HashMap<>();
		map.put("p50" + tags, json.createValue(snapshot.getMedian())); //$NON-NLS-1$
		map.put("p75" + tags, json.createValue(snapshot.get75thPercentile())); //$NON-NLS-1$
		map.put("p95" + tags, json.createValue(snapshot.get95thPercentile())); //$NON-NLS-1$
		map.put("p98" + tags, json.createValue(snapshot.get98thPercentile())); //$NON-NLS-1$
		map.put("p99" + tags, json.createValue(snapshot.get99thPercentile())); //$NON-NLS-1$
		map.put("p999" + tags, json.createValue(snapshot.get999thPercentile())); //$NON-NLS-1$
		map.put("min" + tags, json.createValue(snapshot.getMin())); //$NON-NLS-1$
		map.put("mean" + tags, json.createValue(snapshot.getMean())); //$NON-NLS-1$
		map.put("max" + tags, json.createValue(snapshot.getMax())); //$NON-NLS-1$
		map.put("stddev" + tags, json.createValue(snapshot.getStdDev())); //$NON-NLS-1$
		return map;
	}

	private Map<String, JsonValue> snapshotValues(Snapshot snapshot, String unit, String tags) {
		Map<String, JsonValue> map = new HashMap<>();
		map.put("p50" + tags, json.createValue(toBase(snapshot.getMedian(), unit))); //$NON-NLS-1$
		map.put("p75" + tags, json.createValue(toBase(snapshot.get75thPercentile(), unit))); //$NON-NLS-1$
		map.put("p95" + tags, json.createValue(toBase(snapshot.get95thPercentile(), unit))); //$NON-NLS-1$
		map.put("p98" + tags, json.createValue(toBase(snapshot.get98thPercentile(), unit))); //$NON-NLS-1$
		map.put("p99" + tags, json.createValue(toBase(snapshot.get99thPercentile(), unit))); //$NON-NLS-1$
		map.put("p999" + tags, json.createValue(toBase(snapshot.get999thPercentile(), unit))); //$NON-NLS-1$
		map.put("min" + tags, json.createValue(toBase(snapshot.getMin(), unit))); //$NON-NLS-1$
		map.put("mean" + tags, json.createValue(toBase(snapshot.getMean(), unit))); //$NON-NLS-1$
		map.put("max" + tags, json.createValue(toBase(snapshot.getMax(), unit))); //$NON-NLS-1$
		map.put("stddev" + tags, json.createValue(toBase(snapshot.getStdDev(), unit))); //$NON-NLS-1$
		return map;
	}

	private Double toBase(Number count, String unit) {
		return ExporterUtil.convertNanosTo(count.doubleValue(), unit);
	}

	private Number getValueFromMetric(Metric theMetric, String name) {
		if (theMetric instanceof Gauge) {
			Number value = (Number) ((Gauge<?>) theMetric).getValue();
			if (value != null) {
				return value;
			} else {
				return 0;
			}
		} else if (theMetric instanceof Counter) {
			return ((Counter) theMetric).getCount();
		} else {
			return null;
		}
	}

	/**
	 * Converts a list of tags to the string that will be appended to the metric
	 * name in JSON output. If there are no tags, this returns an empty string.
	 */
	private String createTagsString(Map<String, String> tags) {
		if (tags == null) {
			return ""; //$NON-NLS-1$
		} else {
			Map<String, String> withGlobalTags = new TreeMap<>(tags);
			withGlobalTags.putAll(globalTags);
			if (withGlobalTags.isEmpty()) {
				return ""; //$NON-NLS-1$
			}
			return ";" + withGlobalTags.entrySet().stream() //$NON-NLS-1$
					.map(tag -> tag.getKey() + "=" + tag.getValue() //$NON-NLS-1$
							.replaceAll(";", "_")) //$NON-NLS-1$ //$NON-NLS-2$
					.collect(Collectors.joining(";")); //$NON-NLS-1$
		}
	}
}
