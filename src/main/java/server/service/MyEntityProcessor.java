package server.service;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.ContextURL.Suffix;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.deserializer.DeserializerResult;
import org.apache.olingo.server.api.deserializer.ODataDeserializer;
import org.apache.olingo.server.api.processor.MediaEntityProcessor;
import org.apache.olingo.server.api.serializer.EntitySerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.queryoption.ExpandOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;

import server.database.DbAccess;

public class MyEntityProcessor implements MediaEntityProcessor {

	private OData odata;
	private ServiceMetadata srvMetadata;
	private DbAccess dbAccess;
	
	public MyEntityProcessor(DbAccess dbAccess) {
        this.dbAccess = dbAccess;
    }

	@Override
	public void init(OData odata, ServiceMetadata srvMetadata) {
		this.odata = odata;
		this.srvMetadata = srvMetadata;
	}

    @Override
	public void readEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
		throws ODataApplicationException, ODataLibraryException {

		Entity 			responseEntity 		  = null; // required for serialization of the response body
		EdmEntitySet 	responseEdmEntitySet  = null; // we need this for building the contextUrl

		List<UriResource> resourceParts 	= uriInfo.getUriResourceParts();

		// Note: only in our example we can assume that the first segment is the EntitySet
		UriResource uriResource 			= resourceParts.get(0);

		if (!(uriResource instanceof UriResourceEntitySet)) {
			throw new ODataApplicationException("Only EntitySet is supported", HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ROOT);
		}

		UriResourceEntitySet uriResourceEntitySet 	= (UriResourceEntitySet) uriResource;
		EdmEntitySet 		 startEdmEntitySet 		= uriResourceEntitySet.getEntitySet();

		responseEdmEntitySet 	= startEdmEntitySet; // since we have only one segment

		// 2. step: retrieve the data from backend
		List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();

		responseEntity = dbAccess.readEntityData(startEdmEntitySet, keyPredicates);

		if (responseEntity == null) {
			throw new ODataApplicationException("Nothing found.", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ROOT);
		}

		// handle $select
		SelectOption selectOption = uriInfo.getSelectOption();

		// handle $expand Not implemented
		ExpandOption expandOption = uriInfo.getExpandOption();

		// 4. serialize
		EdmEntityType edmEntityType = responseEdmEntitySet.getEntityType();
		
		// we need the property names of the $select, in order to build the context URL
		String selectList = odata.createUriHelper().buildContextURLSelectList(edmEntityType, expandOption, selectOption);
		ContextURL contextUrl = ContextURL.with().entitySet(responseEdmEntitySet).selectList(selectList).suffix(Suffix.ENTITY).build();

		// make sure that $expand and $select are considered by the serializer
		// adding the selectOption to the serializerOpts will actually tell the lib to do the job
		EntitySerializerOptions opts = EntitySerializerOptions.with().contextURL(contextUrl).select(selectOption).expand(expandOption).build();
		ODataSerializer serializer = this.odata.createSerializer(responseFormat);
		SerializerResult serializerResult = serializer.entity(srvMetadata, edmEntityType, responseEntity, opts);

		// 5. configure the response object
		response.setContent(serializerResult.getContent());
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());

	}

    @Override
	public void createEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat,
		ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {

		 // 1. Retrieve the entity type from the URI
		 EdmEntitySet edmEntitySet = Util.getEdmEntitySet(uriInfo);
		 EdmEntityType edmEntityType = edmEntitySet.getEntityType();

		 // 2. create the data in backend
		// 2.1. retrieve the payload from the POST request for the entity to create and deserialize it
		InputStream requestInputStream = request.getBody();
		ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);
		DeserializerResult result = deserializer.entity(requestInputStream, edmEntityType);
		Entity requestEntity = result.getEntity();

		// 2.2 do the creation in backend, which returns the newly created entity
		Entity createdEntity = dbAccess.createEntityData(edmEntitySet, requestEntity);

		// 3. serialize the response (we have to return the created entity)
		ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();

		// expand and select currently not supported
		EntitySerializerOptions options = EntitySerializerOptions.with().contextURL(contextUrl).build();

		ODataSerializer serializer = this.odata.createSerializer(responseFormat);
		SerializerResult serializedResponse = serializer.entity(srvMetadata, edmEntityType, createdEntity, options);
		  
		//4. configure the response object
		response.setContent(serializedResponse.getContent());
		response.setStatusCode(HttpStatusCode.CREATED.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
	}
	
	@Override
	public void updateEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType requestFormat,
		ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {

		// 1. Retrieve the entity set which belongs to the requested entity
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		// Note: only in our example we can assume that the first segment is the EntitySet
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();
		EdmEntityType edmEntityType = edmEntitySet.getEntityType();
	  
		// 2. update the data in backend
		// 2.1. retrieve the payload from the PUT request for the entity to be updated
		InputStream requestInputStream = request.getBody();
		ODataDeserializer deserializer = this.odata.createDeserializer(requestFormat);
		DeserializerResult result = deserializer.entity(requestInputStream, edmEntityType);
		Entity requestEntity = result.getEntity();
		// 2.2 do the modification in backend
		List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();
		// Note that this updateEntity()-method is invoked for both PUT or PATCH operations
		HttpMethod httpMethod = request.getMethod();
		dbAccess.updateEntityData(edmEntitySet, keyPredicates, requestEntity, httpMethod);
	  
		//3. configure the response object
		response.setStatusCode(HttpStatusCode.NO_CONTENT.getStatusCode());
	}

	@Override
	public void deleteEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {

	}

	@Override
	public void readMediaEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {

		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();

		if (resourcePaths.get(0) instanceof UriResourceEntitySet) {

			final UriResourceEntitySet 	uriResource 		 = (UriResourceEntitySet) resourcePaths.get(0);
			final EdmEntitySet 			edmEntitySet 		 = uriResource.getEntitySet();

			final UriResourceEntitySet 	uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);

			final Entity entity = dbAccess.readEntityData(edmEntitySet, uriResourceEntitySet.getKeyPredicates());

			if (entity == null) {
				throw new ODataApplicationException("Entity not found", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
			}

			final byte[] mediaContent = dbAccess.readMedia(entity);
			final InputStream responseContent = odata.createFixedFormatSerializer().binary(mediaContent);

			response.setContent(responseContent);
			response.setStatusCode(HttpStatusCode.OK.getStatusCode());
			response.setHeader(HttpHeader.CONTENT_TYPE, entity.getMediaContentType());
		
		} 
		else {
			throw new ODataApplicationException("Not implemented", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ENGLISH);
		}
	}

	@Override
	public void createMediaEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType requestFormat, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
	}

	@Override
	public void updateMediaEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType requestFormat, ContentType responseFormat)
			throws ODataApplicationException, ODataLibraryException {
	}

	@Override
	public void deleteMediaEntity(ODataRequest request, ODataResponse response, UriInfo uriInfo)
			throws ODataApplicationException, ODataLibraryException {
	}

}