package dev.qrowned.npc.api.data;

import com.google.common.base.Preconditions;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class NPCData implements Cloneable {

    private static final ThreadLocal<Gson> GSON = ThreadLocal
            .withInitial(() -> new GsonBuilder().serializeNulls().create());

    private static final String UUID_REQUEST_URL = "https://api.mojang.com/users/profiles/minecraft/%s";
    private static final String TEXTURES_REQUEST_URL = "https://sessionserver.mojang.com/session/minecraft/profile/%s?unsigned=%b";

    private static final Pattern UNIQUE_ID_PATTERN = Pattern
            .compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");
    private static final Type PROPERTY_LIST_TYPE = TypeToken
            .getParameterized(Set.class, Property.class).getType();

    private String name;
    private UUID uniqueId;
    private Collection<Property> properties;

    /**
     * Creates a new data. Either {@code uniqueId} or {@code name} must be non-null.
     *
     * @param uniqueId   The unique id of the data.
     * @param name       The name of the data.
     * @param properties The properties of the data.
     */
    private NPCData(UUID uniqueId, String name, Collection<Property> properties) {
        Preconditions
                .checkArgument(name != null || uniqueId != null, "Either name or uniqueId must be given!");

        this.uniqueId = uniqueId;
        this.name = name;
        this.properties = properties;
    }

    /**
     * Creates a new data.
     *
     * @param uniqueId The unique id of the data.
     */
    public static NPCData create(@NotNull UUID uniqueId) {
        return create(uniqueId, null);
    }

    /**
     * Creates a new data.
     *
     * @param uniqueId   The unique id of the data.
     * @param properties The properties of the data.
     */
    public static NPCData create(@NotNull UUID uniqueId, Collection<Property> properties) {
        return new NPCData(uniqueId, null, properties);
    }

    /**
     * Creates a new data.
     *
     * @param name The name of the data.
     */
    public static NPCData create(@NotNull String name) {
        return create(name, null);
    }

    /**
     * Creates a new data.
     *
     * @param name       The name of the data.
     * @param properties The properties of the data.
     */
    public static NPCData create(@NotNull String name, Collection<Property> properties) {
        return new NPCData(null, name, properties);
    }

    /**
     * Creates a new data.
     *
     * @param uniqueId   The uuid of the data.
     * @param name       The name of the data.
     * @param properties The properties of the data.
     */
    public static NPCData create(UUID uniqueId, String name, Collection<Property> properties) {
        return new NPCData(uniqueId, name, properties);
    }

    /**
     * Checks if this data is complete. Complete does not mean, that the data has textures.
     *
     * @return if this data is complete (has unique id and name)
     */
    public boolean isComplete() {
        return this.uniqueId != null && this.name != null;
    }

    /**
     * Checks if this data has textures. That does not mean, that this data has a name and
     * unique id.
     *
     * @return if this data has textures.
     */
    public boolean hasTextures() {
        return this.getProperty("textures").isPresent();
    }

    /**
     * Checks if this data has properties.
     *
     * @return if this data has properties
     */
    public boolean hasProperties() {
        return this.properties != null && !this.properties.isEmpty();
    }

    /**
     * Fills this data with all missing attributes
     *
     * @return if the data was successfully completed
     */
    public boolean complete() {
        return this.complete(true);
    }

    /**
     * Fills this data with all missing attributes
     *
     * @param propertiesAndName if properties and name should be filled for this data
     * @return if the data was successfully completed
     */
    public boolean complete(boolean propertiesAndName) {
        if (this.isComplete() && this.hasProperties()) {
            return true;
        }

        if (this.uniqueId == null) {
            JsonElement identifierElement = this.makeRequest(String.format(UUID_REQUEST_URL, this.name));
            if (identifierElement == null || !identifierElement.isJsonObject()) {
                return false;
            }

            JsonObject jsonObject = identifierElement.getAsJsonObject();
            if (jsonObject.has("id")) {
                this.uniqueId = UUID.fromString(
                        UNIQUE_ID_PATTERN.matcher(jsonObject.get("id").getAsString())
                                .replaceAll("$1-$2-$3-$4-$5"));
            } else {
                return false;
            }
        }

        if ((this.name == null || this.properties == null) && propertiesAndName) {
            JsonElement profileElement = this.makeRequest(
                    String.format(TEXTURES_REQUEST_URL, this.uniqueId.toString().replace("-", ""), false));
            if (profileElement == null || !profileElement.isJsonObject()) {
                return false;
            }

            JsonObject object = profileElement.getAsJsonObject();
            if (object.has("name") && object.has("properties")) {
                this.name = this.name == null ? object.get("name").getAsString() : this.name;
                this.getProperties()
                        .addAll(GSON.get().fromJson(object.get("properties"), PROPERTY_LIST_TYPE));
            } else {
                return false;
            }
        }

        return true;
    }

    /**
     * Makes a request to the given url, accepting only application/json.
     *
     * @param apiUrl The api url to make the request to.
     * @return The json element parsed from the result stream of the site.
     */
    protected @Nullable
    JsonElement makeRequest(@NotNull String apiUrl) {
        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(apiUrl).openConnection();
            connection.setReadTimeout(5000);
            connection.setConnectTimeout(5000);
            connection.setUseCaches(true);
            connection.connect();

            if (connection.getResponseCode() == 200) {
                try (Reader reader = new InputStreamReader(connection.getInputStream(),
                        StandardCharsets.UTF_8)) {
                    return JsonParser.parseReader(reader);
                }
            }
            return null;
        } catch (IOException exception) {
            exception.printStackTrace();
            return null;
        }
    }

    /**
     * Checks if this data has a unique id.
     *
     * @return if this data has a unique id.
     */
    public boolean hasUniqueId() {
        return this.uniqueId != null;
    }

    /**
     * Get the unique id of this data. May be null when this data was created using a name and
     * is not complete. Is never null when {@link #hasUniqueId()} is {@code true}.
     *
     * @return the unique id of this data.
     */
    public UUID getUniqueId() {
        return this.uniqueId;
    }

    /**
     * Sets the unique of this data. To re-request the data textures/uuid of this data, make
     * sure the properties are clear.
     *
     * @param uniqueId the new unique of this data.
     * @return the same data instance, for chaining.
     */
    @NotNull
    public NPCData setUniqueId(UUID uniqueId) {
        this.uniqueId = uniqueId;
        return this;
    }

    /**
     * Check if this data has a name.
     *
     * @return if this data has a name.
     */
    public boolean hasName() {
        return this.name != null;
    }

    /**
     * Get the name of this data. May be null when this data was created using a unique id and
     * is not complete. Is never null when {@link #hasName()} ()} is {@code true}.
     *
     * @return the name of this data.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the name of this data. To re-request the data textures/uuid of this data, make
     * sure the properties are clear.
     *
     * @param name the new name of this data.
     * @return the same data instance, for chaining.
     */
    @NotNull
    public NPCData setName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Gets the properties of this data.
     *
     * @return the properties of this data.
     */
    @NotNull
    public Collection<Property> getProperties() {
        if (this.properties == null) {
            this.properties = ConcurrentHashMap.newKeySet();
        }
        return this.properties;
    }

    /**
     * Sets the properties of this data.
     *
     * @param properties The new properties of this data.
     */
    public void setProperties(Collection<Property> properties) {
        this.properties = properties;
    }

    /**
     * Adds the given {@code property} to this data.
     *
     * @param property the property to add.
     * @return the same data instance, for chaining.
     */
    @NotNull
    public NPCData setProperty(@NotNull Property property) {
        this.getProperties().add(property);
        return this;
    }

    /**
     * Get a specific property by its name.
     *
     * @param name the name of the property.
     * @return the property.
     */
    public @NotNull Optional<Property> getProperty(@NotNull String name) {
        return this.getProperties().stream().filter(property -> property.getName().equals(name))
                .findFirst();
    }

    /**
     * Clears the properties of this data.
     */
    public void clearProperties() {
        this.getProperties().clear();
    }

    /**
     * Creates a clone of this data.
     *
     * @return the cloned data.
     */
    @Override
    public NPCData clone() {
        try {
            return (NPCData) super.clone();
        } catch (CloneNotSupportedException exception) {
            return new NPCData(this.uniqueId, this.name,
                    this.properties == null ? null : new HashSet<>(this.properties));
        }
    }

    /**
     * A property a data can contain. A property must be immutable.
     */
    @Getter
    public static class Property {

        private final String name;
        private final String value;
        private final String signature;

        /**
         * Creates a new data property object.
         *
         * @param name      The name of the data property.
         * @param value     The value of the property.
         * @param signature The signature of the property or null if the property is not signed.
         */
        public Property(@NotNull String name, @NotNull String value, @Nullable String signature) {
            this.name = name;
            this.value = value;
            this.signature = signature;
        }

        /**
         * Get if this property has a signature.
         *
         * @return if this property has a signature.
         */
        public boolean isSigned() {
            return this.signature != null;
        }
    }

}
