package server.service;

import java.io.InputStream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataLibraryException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerResult;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriInfoResource;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourcePrimitiveProperty;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;
import org.apache.olingo.server.api.uri.queryoption.OrderByItem;
import org.apache.olingo.server.api.uri.queryoption.OrderByOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.api.uri.queryoption.SkipOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.apache.olingo.server.api.uri.queryoption.expression.Expression;
import org.apache.olingo.server.api.uri.queryoption.expression.Member;

import server.database.DbAccess;

public class MyEntityCollectionProcessor implements EntityCollectionProcessor {

	private OData odata;
	private ServiceMetadata srvMetadata;
	private DbAccess dbAccess;

	public MyEntityCollectionProcessor(DbAccess dbAccess) {
		this.dbAccess = dbAccess;
	}

	@Override
	public void init(OData odata, ServiceMetadata srvMetadata) {
		this.odata = odata;
		this.srvMetadata = srvMetadata;
	}

	// get entity set, all data
	@Override
	public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo,
		ContentType responseFormat) throws ODataApplicationException, ODataLibraryException {

		// 1st we have retrieve the requested EntitySet from the uriInfo object
		// (representation of the parsed service URI)
		List<UriResource> 		resourcePaths 			= uriInfo.getUriResourceParts();
		UriResourceEntitySet 	uriResourceEntitySet 	= (UriResourceEntitySet) resourcePaths.get(0);
		EdmEntitySet 			edmEntitySet			= uriResourceEntitySet.getEntitySet();

		// 2nd: fetch the data from backend for this requested EntitySetName
		FilterOption filterOption = uriInfo.getFilterOption();

		EntityCollection entitySet = dbAccess.readEntitySetData(edmEntitySet, filterOption);

		// 3rd: apply System Query Options
		// modify the result set according to the query options, specified by the end user
		List<Entity> 		entityList 			   	= entitySet.getEntities();
		EntityCollection 	returnEntityCollection  = new EntityCollection();

		// handle $count: return the original number of entities, ignore $top and $skip
		CountOption countOption = uriInfo.getCountOption();
		if (countOption != null) {
			boolean isCount = countOption.getValue();
			if (isCount) {
				returnEntityCollection.setCount(entityList.size());
			}
		}

		// handle $skip
		SkipOption skipOption = uriInfo.getSkipOption();
		if (skipOption != null) {
			int skipNumber = skipOption.getValue();
			if (skipNumber >= 0) {
				if (skipNumber <= entityList.size()) {
					entityList = entityList.subList(skipNumber, entityList.size());
				} 
				else {
					// The client skipped all entities
					entityList.clear();
				}
			} 
			else {
				throw new ODataApplicationException("Invalid value for $skip", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
			}
		}

		// handle $top
		TopOption topOption = uriInfo.getTopOption();
		if (topOption != null) {
			int topNumber = topOption.getValue();
			if (topNumber >= 0) {
				if (topNumber <= entityList.size()) {
					entityList = entityList.subList(0, topNumber);
				} // else the client has requested more entities than available => return what we have
			} 
			else {
				throw new ODataApplicationException("Invalid value for $top", HttpStatusCode.BAD_REQUEST.getStatusCode(), Locale.ROOT);
			}
		}

		// handle $select
		SelectOption selectOption = uriInfo.getSelectOption();

		// handle $orderBy
		OrderByOption orderByOption = uriInfo.getOrderByOption();
		if ( orderByOption != null ) {
			List<OrderByItem> orderItemList = orderByOption.getOrders();
			final OrderByItem orderByItem = orderItemList.get(0);
			Expression expression = orderByItem.getExpression();
			if(expression instanceof Member){
				UriInfoResource resourcePath = ((Member)expression).getResourcePath();
				UriResource uriResource = resourcePath.getUriResourceParts().get(0);
				if (uriResource instanceof UriResourcePrimitiveProperty) {
					EdmProperty edmProperty = ((UriResourcePrimitiveProperty)uriResource).getProperty();
					final String sortPropertyName = edmProperty.getName();
	
					// do the sorting for the list of entities  
					Collections.sort(entityList, new Comparator<Entity>() {
	
						// delegate the sorting to native sorter of Integer and String
						public int compare(Entity entity1, Entity entity2) {
							int compareResult = 0;
	
							if(sortPropertyName.equals("id")){
								Integer integer1 = (Integer) entity1.getProperty(sortPropertyName).getValue();
								Integer integer2 = (Integer) entity2.getProperty(sortPropertyName).getValue();
	
								compareResult = integer1.compareTo(integer2);
							}
							else {
								String propertyValue1 = (String) entity1.getProperty(sortPropertyName).getValue();
								String propertyValue2 = (String) entity2.getProperty(sortPropertyName).getValue();
	
								compareResult = propertyValue1.compareTo(propertyValue2);
							}
	
							// if 'desc' is specified in the URI, change the order
							if(orderByItem.isDescending()){
								return - compareResult; // just reverse order
							}
	
							return compareResult;
						}

					});
				}
			}
		}

		// after applying the query options, create EntityCollection based on the reduced list
		for (Entity entity : entityList) {
			returnEntityCollection.getEntities().add(entity);
		}

		// 4th: create a serializer based on the requested format (json)
		ODataSerializer serializer 	= odata.createSerializer(responseFormat);

		// and serialize the content: transform from the EntitySet object to InputStream
		EdmEntityType edmEntityType = edmEntitySet.getEntityType();

		// we need the property names of the $select, in order to build the context URL
		String selectList 			= odata.createUriHelper().buildContextURLSelectList(edmEntityType, null, selectOption);
		ContextURL contextUrl 		= ContextURL.with().entitySet(edmEntitySet).selectList(selectList).build();

		final String id 						= request.getRawBaseUri() + "/" + edmEntitySet.getName();
		EntityCollectionSerializerOptions opts  = EntityCollectionSerializerOptions.with().contextURL(contextUrl).select(selectOption).id(id).count(countOption).build();
		SerializerResult serializerResult 		= serializer.entityCollection(srvMetadata, edmEntityType, returnEntityCollection, opts);
		InputStream serializedContent 			= serializerResult.getContent();

		// 5th: configure the response object: set the body, headers and status code
		response.setContent(serializedContent);
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());

	}

}