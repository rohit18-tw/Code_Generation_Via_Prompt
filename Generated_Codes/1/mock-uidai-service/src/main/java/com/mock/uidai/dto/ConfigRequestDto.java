package com.mock.uidai.dto;

import java.io.Serializable;
import java.util.Objects;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.springframework.validation.annotation.Validated;

/**
* Data Transfer Object for configuration requests in the mock UIDAI service.
* This class represents the structure of configuration data that can be sent to or
* received from the UIDAI service API.
*/
@Validated
public class ConfigRequestDto implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "Config key cannot be blank")
    @Size(max = 100, message = "Config key must not exceed 100 characters")
    private String key;

    @NotBlank(message = "Config value cannot be blank")
    @Size(max = 500, message = "Config value must not exceed 500 characters")
    private String value;

    @NotNull(message = "Active status cannot be null")
    private Boolean active;

    @Size(max = 255, message = "Description must not exceed 255 characters")
    private String description;

    @Pattern(regexp = "^(SYSTEM|USER|API)$", message = "Source must be one of: SYSTEM, USER, API")
    private String source;

    /**
    * Default constructor for ConfigRequestDto.
    */
    public ConfigRequestDto() {
    }

    /**
    * Parameterized constructor for ConfigRequestDto.
    *
    * @param key         The configuration key
    * @param value       The configuration value
    * @param active      Whether the configuration is active
    * @param description Description of the configuration
    * @param source      Source of the configuration (SYSTEM, USER, API)
    */
    public ConfigRequestDto(String key, String value, Boolean active, String description, String source) {
        this.key = key;
        this.value = value;
        this.active = active;
        this.description = description;
        this.source = source;
    }

    /**
    * Gets the configuration key.
    *
    * @return The configuration key
    */
    public String getKey() {
        return key;
    }

    /**
    * Sets the configuration key.
    *
    * @param key The configuration key to set
    */
    public void setKey(String key) {
        this.key = key;
    }

    /**
    * Gets the configuration value.
    *
    * @return The configuration value
    */
    public String getValue() {
        return value;
    }

    /**
    * Sets the configuration value.
    *
    * @param value The configuration value to set
    */
    public void setValue(String value) {
        this.value = value;
    }

    /**
    * Gets the active status of the configuration.
    *
    * @return The active status
    */
    public Boolean getActive() {
        return active;
    }

    /**
    * Sets the active status of the configuration.
    *
    * @param active The active status to set
    */
    public void setActive(Boolean active) {
        this.active = active;
    }

    /**
    * Gets the description of the configuration.
    *
    * @return The description
    */
    public String getDescription() {
        return description;
    }

    /**
    * Sets the description of the configuration.
    *
    * @param description The description to set
    */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
    * Gets the source of the configuration.
    *
    * @return The source
    */
    public String getSource() {
        return source;
    }

    /**
    * Sets the source of the configuration.
    *
    * @param source The source to set
    */
    public void setSource(String source) {
        this.source = source;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConfigRequestDto that = (ConfigRequestDto) o;
        return Objects.equals(key, that.key) &&
        Objects.equals(value, that.value) &&
        Objects.equals(active, that.active) &&
        Objects.equals(description, that.description) &&
        Objects.equals(source, that.source);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value, active, description, source);
    }

    @Override
    public String toString() {
        return "ConfigRequestDto{" +
        "key='" + key + '\'' +
        ", value='" + (value != null ? "****" : null) + '\'' +  // Masking value for security
        ", active=" + active +
        ", description='" + description + '\'' +
        ", source='" + source + '\'' +
        '}';
    }
}