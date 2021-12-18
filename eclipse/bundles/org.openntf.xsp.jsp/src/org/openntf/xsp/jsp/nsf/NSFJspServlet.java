/**
 * Copyright © 2018-2021 Jesse Gallagher
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
package org.openntf.xsp.jsp.nsf;

import java.io.IOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.jasper.servlet.JspServlet;

import com.ibm.designer.runtime.domino.adapter.ComponentModule;

/**
 * 
 * @author Jesse Gallagher
 * @since 2.1.0
 */
public class NSFJspServlet extends JspServlet {
	private static final long serialVersionUID = 1L;
	
	@SuppressWarnings("unused")
	private final ComponentModule module;
	
	public NSFJspServlet(ComponentModule module) {
		super();
		this.module = module;
	}

	@Override
	public void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		try {
			super.service(req, resp);
		} catch(Throwable t) {
			t.printStackTrace();
			throw t;
		} finally {
			// Looks like Jasper doesn't flush this on its own
			resp.getWriter().flush();
		}
	}

}
