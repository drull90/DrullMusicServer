package server.entities;

import org.apache.olingo.commons.api.edm.FullQualifiedName;

public class EntityConfig {

    // Service Namespace
    public static final String NAMESPACE                    = "Music";

    // EDM Container
    public static final String CONTAINER_NAME               = "Container";
    public static final FullQualifiedName CONTAINER         = new FullQualifiedName(NAMESPACE, CONTAINER_NAME);

    // Entity Types Names
    public static final String ET_CANCION_NAME              = "Cancion";
    public static final FullQualifiedName ET_CANCION_FQN    = new FullQualifiedName(NAMESPACE, ET_CANCION_NAME);

    // Entity Set Names
    public static final String ES_CANCIONES_NAME             = "Canciones";

}