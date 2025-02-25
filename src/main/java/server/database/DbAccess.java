package server.database;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Locale;

import server.entities.EntityConfig;
import server.service.MyFilterExpressionVisitor;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntityCollection;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.ex.ODataRuntimeException;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.queryoption.FilterOption;

public class DbAccess {

    private Connection connection;
    private Statement statement;

    public DbAccess() {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
            connection = DriverManager.getConnection("jdbc:mariadb://localhost:3306/Music", "Music", "Musicpassword");
            statement = connection.createStatement();
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

    /* PUBLIC FACADE */
    public EntityCollection readEntitySetData(EdmEntitySet edmEntitySet, FilterOption filterOption) throws ODataApplicationException{

        // actually, this is only required if we have more than one Entity Sets	
		switch(edmEntitySet.getName()) {

			case EntityConfig.ES_CANCIONES_NAME:
				return getCanciones(filterOption);

			case EntityConfig.ES_LISTAREPROD_NAME:
				return null;

			default:
				return null;
		}
    }

    public Entity readEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams) throws ODataApplicationException{

        EdmEntityType edmEntityType = edmEntitySet.getEntityType();

        // actually, this is only required if we have more than one Entity Type
		switch(edmEntityType.getName()) {

			case EntityConfig.ET_CANCION_NAME:
				return getCancion(edmEntityType, keyParams);

			case EntityConfig.ET_LISTAREPROD_NAME:
				return null;

			default:
				return null;

		}
	}
	
	public Entity createEntityData(EdmEntitySet edmEntitySet, Entity requestEntity) {

		EdmEntityType edmEntityType = edmEntitySet.getEntityType();

		if(edmEntityType.getName().equals(EntityConfig.ET_SOLICITUD_NAME)) {
			return createSolicitud(edmEntityType, requestEntity);
		}

		return null;
	}

	public void updateEntityData(EdmEntitySet edmEntitySet, List<UriParameter> keyParams, Entity updateEntity,
		HttpMethod httpMethod) throws ODataApplicationException {

	}


	// PRIVATE SECTION
	private Entity createSolicitud(EdmEntityType edmEntityType, Entity entity) {

		Property Autor 	= entity.getProperty("Autor");
		Property Titulo	= entity.getProperty("Titulo");
		Property Album 	= entity.getProperty("Album");
		Property Nick 	= entity.getProperty("Nick");

		String query;
		int newId = -1;

		if(!Album.getValue().equals(""))
			query = "INSERT INTO Solicitudes (Autor, Titulo, Album, Nick) VALUES ";
		else	
			query = "INSERT INTO Solicitudes (Autor, Titulo, Nick) VALUES ";
			
		query += "('" + Autor.getValue().toString() + "','" + Titulo.getValue().toString() + "',";
		
		if(!Album.getValue().equals(""))
			query += "'" + Album.getValue().toString() + "',";
		
		query += "'" + Nick.getValue().toString() + "')";

		try {
			statement.executeUpdate(query, Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = statement.getGeneratedKeys();
			if(rs.next())
				newId = rs.getInt(1);
		} catch (SQLException e) {e.printStackTrace();}

		entity.setId(createId("Solicitudes", newId));

		return entity;
	}


    private EntityCollection getCanciones(FilterOption filterOption) {
		EntityCollection retEntitySet = new EntityCollection();
		
		String query = "SELECT id, Titulo, Autor, Duracion FROM Canciones ";

		if ( filterOption != null ) {
			query += "WHERE " + MyFilterExpressionVisitor.transformToQuery(filterOption);
		}

        ResultSet rs = null;
        try {
			rs =  statement.executeQuery(query);
			while (rs.next()) {

				int id 			= rs.getInt("id");
				String Titulo 	= rs.getString("Titulo");
				String Autor 	= rs.getString("Autor");
				int Duracion 	= rs.getInt("Duracion");

				Entity entity = new Entity();

				entity.addProperty(new Property(null, "id", 		ValueType.PRIMITIVE, id));
				entity.addProperty(new Property(null, "Titulo", 	ValueType.PRIMITIVE, Titulo));
				entity.addProperty(new Property(null, "Autor", 		ValueType.PRIMITIVE, Autor));
				entity.addProperty(new Property(null, "Duracion", 	ValueType.PRIMITIVE, Duracion));

				entity.setMediaContentType("audio/mpeg");

				entity.setId(createId("Canciones", id));
				retEntitySet.getEntities().add(entity);
				entity = null;

			}
			rs.close();
		} 
		catch (SQLException e) { e.printStackTrace();}

        return retEntitySet;
    }

	private Entity getCancion(EdmEntityType edmEntityType, List<UriParameter> keyParams) throws ODataApplicationException {

		/* generic approach to find the requested entity */
		int id 					= -1;
		String Titulo 			= null;
		String Autor 			= null;
		int Duracion 			= 0;
		Entity requestedEntity  = null;
		ResultSet rs 			= null;

		try {
			rs 		= statement.executeQuery("SELECT id, Titulo, Autor, Duracion FROM Canciones WHERE ( " + keyParams.get(0).getName() + " = "
					+ keyParams.get(0).getText() + " )");
			rs.next();
			id 		 = rs.getInt("id");
			Titulo   = rs.getString("Titulo");
			Autor 	 = rs.getString("Autor");
			Duracion = rs.getInt("Duracion");
			rs.close();
		} 
		catch (SQLException e) {
			e.printStackTrace();
		}

		if (id != -1) {
			requestedEntity = new Entity();
			requestedEntity.addProperty(new Property(null, "id", 	 	ValueType.PRIMITIVE, id));
			requestedEntity.addProperty(new Property(null, "Titulo", 	ValueType.PRIMITIVE, Titulo));
			requestedEntity.addProperty(new Property(null, "Autor",  	ValueType.PRIMITIVE, Autor));
			requestedEntity.addProperty(new Property(null, "Duracion", 	ValueType.PRIMITIVE, Duracion));

			requestedEntity.setMediaContentType("audio/mpeg");
			requestedEntity.setId(createId("Canciones", id));
		}
		else {
			// Throw suitable exception if not exist
			throw new ODataApplicationException("Entity for requested key doesn't exist", HttpStatusCode.NOT_FOUND.getStatusCode(), Locale.ENGLISH);
		}

		return requestedEntity;
	}

	private URI createId(String entitySetName, Object id) {
		try {
			return new URI(entitySetName + "(" + String.valueOf(id) + ")");
		} catch (URISyntaxException e) {
			throw new ODataRuntimeException("Unable to create id for entity: " + entitySetName, e);
		}
	}

	public byte[] readMedia(final Entity entity) {

		String 		id 		= entity.getProperty("id").toString();
		byte[] 		stream	= null;
		ResultSet 	rs 		= null;

		try {
			rs 		= statement.executeQuery("SELECT cancion FROM Canciones WHERE " + id);
			rs.next();
			stream = rs.getBytes("cancion");
			rs.close();
		} 
		catch (SQLException e) { e.printStackTrace();}

		return stream;
	}

}