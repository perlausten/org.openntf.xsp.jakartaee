package rest;

import jakarta.inject.Inject;
import jakarta.json.JsonObject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import model.NamedDoc;

@Path("nosqlNamedDocs")
public class NoSQLNamedDocs {
	@Inject
	private NamedDoc.Repository repository;
	
	@Path("{name}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public NamedDoc getNamedDoc(@PathParam("name") String name) {
		return repository.findNamedDocument(name, null)
			.orElseThrow(() -> new NotFoundException());
	}
	
	@Path("{name}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public NamedDoc updateNamedDoc(@PathParam("name") String name, NamedDoc entity) {
		entity.setNoteName(name);
		entity.setDocumentId(null);
		entity.setNoteUserName(null);
		return repository.save(entity);
	}
	
	@Path("{name}")
	@PATCH
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public NamedDoc patchNamedDoc(@PathParam("name") String name, JsonObject patch) {
		NamedDoc doc = getNamedDoc(name);
		doc.setSubject(patch.getString("subject", ""));
		return repository.save(doc);
	}
	
	@Path("{name}/{userName}")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public NamedDoc getNamedDoc(@PathParam("name") String name, @PathParam("userName") String userName) {
		return repository.findNamedDocument(name, userName.replace('+', ' '))
			.orElseThrow(() -> new NotFoundException());
	}
	
	@Path("{name}/{userName}")
	@PUT
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public NamedDoc updateNamedDoc(@PathParam("name") String name, @PathParam("userName") String userName, NamedDoc entity) {
		entity.setNoteName(name);
		entity.setDocumentId(null);
		entity.setNoteUserName(userName.replace('+', ' '));
		return repository.save(entity);
	}
	
	@Path("{name}/{userName}")
	@PATCH
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public NamedDoc patchNamedDoc(@PathParam("name") String name, @PathParam("userName") String userName, JsonObject patch) {
		NamedDoc doc = getNamedDoc(name, userName);
		doc.setSubject(patch.getString("subject", ""));
		return repository.save(doc);
	}
}
