package server.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import server.entities.EntityConfig;

import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.api.edm.provider.CsdlAbstractEdmProvider;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainer;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityContainerInfo;
import org.apache.olingo.commons.api.edm.provider.CsdlEntitySet;
import org.apache.olingo.commons.api.edm.provider.CsdlEntityType;
import org.apache.olingo.commons.api.edm.provider.CsdlProperty;
import org.apache.olingo.commons.api.edm.provider.CsdlPropertyRef;
import org.apache.olingo.commons.api.edm.provider.CsdlSchema;
import org.apache.olingo.commons.api.ex.ODataException;

public class MyEdmProvider extends CsdlAbstractEdmProvider {

    @Override
    public CsdlEntityType getEntityType(FullQualifiedName entityTypeName) throws ODataException {
        
        // this method is called for one of the EntityTypes that are configured in the Schema
        if (entityTypeName.equals(EntityConfig.ET_CANCION_FQN)) {
            // create EntityType properties
            CsdlProperty id       = new CsdlProperty().setName("id").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            CsdlProperty Titulo   = new CsdlProperty().setName("Titulo").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            CsdlProperty Autor    = new CsdlProperty().setName("Autor").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            CsdlProperty Duracion = new CsdlProperty().setName("Duracion").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

            // create CsdlPropertyRef for Key element
            CsdlPropertyRef propertyRef = new CsdlPropertyRef();
            propertyRef.setName("id");

            // configure EntityType
            CsdlEntityType entityType 	= new CsdlEntityType();
            entityType.setName(EntityConfig.ET_CANCION_NAME);
            entityType.setProperties(Arrays.asList(id, Titulo, Autor, Duracion));
            entityType.setKey(Collections.singletonList(propertyRef));
            entityType.setHasStream(true);

            return entityType;
        }
        /*
        if( entityTypeName.equals(EntityConfig.ET_USER_FQN)) {
            // create EntityType properties
            CsdlProperty id             = new CsdlProperty().setName("ID").setType(EdmPrimitiveTypeKind.Int32.getFullQualifiedName());
            CsdlProperty name           = new CsdlProperty().setName("Name").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
            CsdlProperty description    = new CsdlProperty().setName("Description").setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());

            // create CsdlPropertyRef for Key element
            CsdlPropertyRef propertyRef = new CsdlPropertyRef();
            propertyRef.setName("ID");

            // configure EntityType
            CsdlEntityType entityType 	= new CsdlEntityType();
            entityType.setName(EntityConfig.ET_USER_NAME);
            entityType.setProperties(Arrays.asList(id, name, description));
            entityType.setKey(Collections.singletonList(propertyRef));

            return entityType;
        }*/
        
        return null;
    }

    //localhost:8080/Server/MyService/xxxx
    @Override
    public CsdlEntitySet getEntitySet(FullQualifiedName entityContainer, String entitySetName) throws ODataException {

        if (entityContainer.equals(EntityConfig.CONTAINER)) {

            if (entitySetName.equals(EntityConfig.ES_CANCIONES_NAME)) {
                CsdlEntitySet entitySet = new CsdlEntitySet();
                entitySet.setName(EntityConfig.ES_CANCIONES_NAME);
                entitySet.setType(EntityConfig.ET_CANCION_FQN);
                return entitySet;
            }
		
		/*
            if (entitySetName.equals(EntityConfig.ES_USUARIOS_NAME)) {
                CsdlEntitySet entitySet = new CsdlEntitySet();
                entitySet.setName(EntityConfig.ES_USUARIOS_NAME);
                entitySet.setType(EntityConfig.ET_USER_FQN);
                return entitySet;
            }*/
            
        }

        return null;
    }

    @Override
    public List<CsdlSchema> getSchemas() throws ODataException {

        // create Schema
        CsdlSchema schema = new CsdlSchema();
        schema.setNamespace(EntityConfig.NAMESPACE);

        // add EntityTypes
        List<CsdlEntityType> entityTypes = new ArrayList<CsdlEntityType>();
		entityTypes.add(getEntityType(EntityConfig.ET_CANCION_FQN));
		//entityTypes.add(getEntityType(EntityConfig.ET_USER_FQN));
        schema.setEntityTypes(entityTypes);

        // add EntityContainer
        schema.setEntityContainer(getEntityContainer());

        // finally
        List<CsdlSchema> schemas = new ArrayList<CsdlSchema>();
        schemas.add(schema);

        return schemas;
    }

    @Override
    public CsdlEntityContainer getEntityContainer() throws ODataException {
        // create EntitySets
        List<CsdlEntitySet> entitySets = new ArrayList<CsdlEntitySet>();
        entitySets.add(getEntitySet(EntityConfig.CONTAINER, EntityConfig.ES_CANCIONES_NAME));
		//entitySets.add(getEntitySet(EntityConfig.CONTAINER, EntityConfig.ES_USUARIOS_NAME));

        // create EntityContainer
        CsdlEntityContainer entityContainer = new CsdlEntityContainer();
        entityContainer.setName(EntityConfig.CONTAINER_NAME);
        entityContainer.setEntitySets(entitySets);

        return entityContainer;

	}
	
	@Override
    public CsdlEntityContainerInfo getEntityContainerInfo(FullQualifiedName entityContainerName) throws ODataException {

        // This method is invoked when displaying the Service Document at localhost:8080/Server/MyService.svc
        if (entityContainerName == null || entityContainerName.equals(EntityConfig.CONTAINER)) {
            CsdlEntityContainerInfo entityContainerInfo = new CsdlEntityContainerInfo();
            entityContainerInfo.setContainerName(EntityConfig.CONTAINER);
            return entityContainerInfo;
        }

        return null;
    }

}