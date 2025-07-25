package com.mock.uidai.service;

import com.mock.uidai.dto.ConfigRequestDto;
import com.mock.uidai.entity.MockConfig;
import com.mock.uidai.exception.ConfigNotFoundException;
import com.mock.uidai.exception.InvalidConfigException;
import com.mock.uidai.repository.MockConfigRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
* Service class for managing mock configurations.
* Provides methods to create, retrieve, update, and delete mock configurations.
*/
@Service
public class MockConfigService {

    private static final Logger logger = LoggerFactory.getLogger(MockConfigService.class);
    private final MockConfigRepository mockConfigRepository;
    private final ReadWriteLock configLock = new ReentrantReadWriteLock();

    @Autowired
    public MockConfigService(MockConfigRepository mockConfigRepository) {
        this.mockConfigRepository = mockConfigRepository;
    }

    /**
    * Creates a new mock configuration.
    *
    * @param configRequestDto the configuration request data
    * @return the created mock configuration
    * @throws InvalidConfigException if the configuration is invalid
    */
    @Transactional
    public MockConfig createConfig(ConfigRequestDto configRequestDto) {
        logger.info("Creating new mock configuration with key: {}", configRequestDto.getKey());

        validateConfigRequest(configRequestDto);

        configLock.writeLock().lock();
        try {
            // Check if config with the same key already exists
            if (mockConfigRepository.findByKey(configRequestDto.getKey()).isPresent()) {
                logger.error("Configuration with key {} already exists", configRequestDto.getKey());
                throw new InvalidConfigException("Configuration with key " + configRequestDto.getKey() + " already exists");
            }

            MockConfig mockConfig = new MockConfig();
            mockConfig.setKey(configRequestDto.getKey());
            mockConfig.setValue(configRequestDto.getValue());
            mockConfig.setDescription(configRequestDto.getDescription());
            mockConfig.setSource(configRequestDto.getSource());
            mockConfig.setCreatedAt(LocalDateTime.now());
            mockConfig.setUpdatedAt(LocalDateTime.now());

            MockConfig savedConfig = mockConfigRepository.save(mockConfig);
            logger.info("Successfully created mock configuration with ID: {}", savedConfig.getId());
            return savedConfig;
        } finally {
            configLock.writeLock().unlock();
        }
    }

    /**
    * Retrieves a mock configuration by its key.
    *
    * @param key the configuration key
    * @return the mock configuration
    * @throws ConfigNotFoundException if the configuration is not found
    */
    @Cacheable(value = "configCache", key = "#key")
    public MockConfig getConfigByKey(String key) {
        logger.info("Retrieving mock configuration with key: {}", key);

        configLock.readLock().lock();
        try {
            Optional<MockConfig> configOptional = mockConfigRepository.findByKey(key);
            if (configOptional.isPresent()) {
                logger.debug("Found mock configuration with key: {}", key);
                return configOptional.get();
            } else {
                logger.error("Mock configuration with key {} not found", key);
                throw new ConfigNotFoundException("Configuration with key " + key + " not found");
            }
        } finally {
            configLock.readLock().unlock();
        }
    }

    /**
    * Retrieves all mock configurations.
    *
    * @return list of all mock configurations
    */
    public List<MockConfig> getAllConfigs() {
        logger.info("Retrieving all mock configurations");

        configLock.readLock().lock();
        try {
            List<MockConfig> configs = mockConfigRepository.findAll();
            logger.debug("Found {} mock configurations", configs.size());
            return configs;
        } finally {
            configLock.readLock().unlock();
        }
    }

    /**
    * Updates an existing mock configuration.
    *
    * @param key the configuration key
    * @param configRequestDto the updated configuration data
    * @return the updated mock configuration
    * @throws ConfigNotFoundException if the configuration is not found
    * @throws InvalidConfigException if the configuration is invalid
    */
    @Transactional
    @CacheEvict(value = "configCache", key = "#key")
    public MockConfig updateConfig(String key, ConfigRequestDto configRequestDto) {
        logger.info("Updating mock configuration with key: {}", key);

        validateConfigRequest(configRequestDto);

        configLock.writeLock().lock();
        try {
            Optional<MockConfig> configOptional = mockConfigRepository.findByKey(key);
            if (configOptional.isPresent()) {
                MockConfig existingConfig = configOptional.get();

                // If the key in the request is different from the path parameter and already exists
                if (!key.equals(configRequestDto.getKey()) &&
                mockConfigRepository.findByKey(configRequestDto.getKey()).isPresent()) {
                    logger.error("Cannot update key to {} as it already exists", configRequestDto.getKey());
                    throw new InvalidConfigException("Configuration with key " + configRequestDto.getKey() + " already exists");
                }

                existingConfig.setKey(configRequestDto.getKey());
                existingConfig.setValue(configRequestDto.getValue());
                existingConfig.setDescription(configRequestDto.getDescription());
                existingConfig.setSource(configRequestDto.getSource());
                existingConfig.setUpdatedAt(LocalDateTime.now());

                MockConfig updatedConfig = mockConfigRepository.save(existingConfig);
                logger.info("Successfully updated mock configuration with ID: {}", updatedConfig.getId());
                return updatedConfig;
            } else {
                logger.error("Mock configuration with key {} not found for update", key);
                throw new ConfigNotFoundException("Configuration with key " + key + " not found");
            }
        } finally {
            configLock.writeLock().unlock();
        }
    }

    /**
    * Deletes a mock configuration by its key.
    *
    * @param key the configuration key
    * @throws ConfigNotFoundException if the configuration is not found
    */
    @Transactional
    @CacheEvict(value = "configCache", key = "#key")
    public void deleteConfig(String key) {
        logger.info("Deleting mock configuration with key: {}", key);

        configLock.writeLock().lock();
        try {
            Optional<MockConfig> configOptional = mockConfigRepository.findByKey(key);
            if (configOptional.isPresent()) {
                mockConfigRepository.delete(configOptional.get());
                logger.info("Successfully deleted mock configuration with key: {}", key);
            } else {
                logger.error("Mock configuration with key {} not found for deletion", key);
                throw new ConfigNotFoundException("Configuration with key " + key + " not found");
            }
        } finally {
            configLock.writeLock().unlock();
        }
    }

    /**
    * Validates the configuration request data.
    *
    * @param configRequestDto the configuration request data to validate
    * @throws InvalidConfigException if the configuration is invalid
    */
    private void validateConfigRequest(ConfigRequestDto configRequestDto) {
        if (configRequestDto == null) {
            logger.error("Configuration request cannot be null");
            throw new InvalidConfigException("Configuration request cannot be null");
        }

        if (configRequestDto.getKey() == null || configRequestDto.getKey().trim().isEmpty()) {
            logger.error("Configuration key cannot be null or empty");
            throw new InvalidConfigException("Configuration key cannot be null or empty");
        }

        if (configRequestDto.getValue() == null) {
            logger.error("Configuration value cannot be null");
            throw new InvalidConfigException("Configuration value cannot be null");
        }
    }
}