package server.service;

import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.PrimitiveProcessor;
import org.apache.olingo.server.api.uri.UriInfo;

import server.database.DbAccess;

public class MyPrimitiveProcessor implements PrimitiveProcessor {

    //private OData odata;
    //private ServiceMetadata serviceMetadata;
    //private Storage storage;

    public MyPrimitiveProcessor(DbAccess dbAccess) {
        //this.storage = storage;
    }

    @Override
    public void init(OData odata, ServiceMetadata serviceMetadata) {
        //this.odata = odata;
        //this.serviceMetadata = serviceMetadata;
    }

    @Override
	public void readPrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
		    throws ODataApplicationException, ODataLibraryException {

    }

    @Override
	public void updatePrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo,
		    ContentType requestFormat, ContentType responseFormat)
		    throws ODataApplicationException, ODataLibraryException {
		
	}

	@Override
	public void deletePrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo)
		    throws ODataApplicationException, ODataLibraryException {
		
	}

}