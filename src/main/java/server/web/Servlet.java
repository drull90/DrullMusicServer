package server.web;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.olingo.commons.api.edmx.EdmxReference;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataHttpHandler;
import org.apache.olingo.server.api.ServiceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import server.database.DbAccess;
import server.database.Login;
import server.service.MyEdmProvider;
import server.service.MyEntityCollectionProcessor;
import server.service.MyEntityProcessor;
import server.service.MyPrimitiveProcessor;

public class Servlet extends HttpServlet {

	private static final long serialVersionUID = 1L;
	private static final Logger LOG = LoggerFactory.getLogger(Servlet.class);

	Login login = null;

	protected void service(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {

		try {
	        HttpSession session = req.getSession(true);
			DbAccess dbAccess 	= (DbAccess) session.getAttribute(DbAccess.class.getName());
			
	        if (dbAccess == null) {
	           dbAccess = new DbAccess();
	           session.setAttribute(DbAccess.class.getName(), dbAccess);
			}

			// create odata handler and configure it with EdmProvider and Processor
			OData odata 			 = OData.newInstance();
			ServiceMetadata edm 	 = odata.createServiceMetadata(new MyEdmProvider(), new ArrayList<EdmxReference>());
			
			ODataHttpHandler handler = odata.createHandler(edm);
			
			handler.register(new MyEntityCollectionProcessor(dbAccess));
			handler.register(new MyEntityProcessor(dbAccess));
			handler.register(new MyPrimitiveProcessor(dbAccess));
			
			handler.process(req, resp);


		}
		catch (RuntimeException e) {
		    LOG.error("Server Error occurred in ExampleServlet", e);
		    throw new ServletException(e);	
		}

	}

}