package com.dailycred.dynamonito.core;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.UUID;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;

import com.amazonaws.services.dynamodb.datamodeling.DynamoDBAutoGeneratedKey;
import com.amazonaws.services.dynamodb.datamodeling.DynamoDBIgnore;
import com.dailycred.dynamonito.cache.InMemoryCacheAdaptor;
import com.dailycred.dynamonito.datamodel.AutoGeneratedKeyModel;
import com.dailycred.dynamonito.datamodel.DataType;
import com.dailycred.dynamonito.util.Util;

public class MapperTest extends BaseTest {

  private static InMemoryCacheAdaptor cacheAdaptor = new InMemoryCacheAdaptor();
  private static DynamonitoMapper mapper = new DynamonitoMapper(getClient(), cacheAdaptor);

  @Test
  public void testMapper() throws Exception {
    DataType model = null;
    DynamonitoMapper mapper = new DynamonitoMapper(getClient(), cacheAdaptor);
    try {
      model = DataType.buildModelWithRandomKey();
      mapper.save(model);
      assertEquals(model, mapper.load(DataType.class, model.getHashKey(), model.getRangeKey()));
      String jsonFromCache = cacheAdaptor.get("DataType", model.getHashKey(), model.getRangeKey());
      DataType modelFromCache = mapper.marshallIntoObject(DataType.class,
          Util.parseItemJson(new ObjectMapper().readTree(jsonFromCache)));
      assertEquals(model, modelFromCache);
    } finally {
      if (model != null) {
        mapper.delete(model);
      }
    }
  }

  /**
   * Test the support of the annotation {@link DynamoDBIgnore}.
   * 
   * @throws Exception
   */
  @Test
  public void testIgnoredAttribute() throws Exception {
    DataType model = null;
    try {
      model = DataType.buildModelWithRandomKey();
      mapper.save(model);
      String jsonFromCache = cacheAdaptor.get("DataType", model.getHashKey(), model.getRangeKey());
      // The cached value should not contain the ignored property.
      assertFalse(jsonFromCache.contains("ignored"));
      DataType modelFromCache = mapper.marshallIntoObject(DataType.class,
          Util.parseItemJson(new ObjectMapper().readTree(jsonFromCache)));
      // The restored model should not contain the ignored property.
      assertTrue(modelFromCache.getIgnored() == null);
    } finally {
      if (model != null) {
        mapper.delete(model);
      }
    }
  }

  /**
   * Test the support of the annotation {@link DynamoDBAutoGeneratedKey}.
   * 
   * @throws Exception
   */
  @Test
  public void testAutoGeneratedKey() throws Exception {
    AutoGeneratedKeyModel model = null;
    try {

      model = new AutoGeneratedKeyModel();
      model.setString("str value \n");

      model.setRangeKey("" + System.currentTimeMillis());
      // we let DDB mapper set hash key for us...
      mapper.save(model);
      String jsonFromCache = cacheAdaptor.get("DataType", model.getHashKey(), model.getRangeKey());
      // The cached value should contain a hash key
      assertTrue(jsonFromCache.contains("hashKey"));
      model = mapper.marshallIntoObject(AutoGeneratedKeyModel.class,
          Util.parseItemJson(new ObjectMapper().readTree(jsonFromCache)));
      // The restored model should contain the UUID hash key. Next line should not fail.
      UUID.fromString(model.getHashKey());
    } finally {
      if (model != null) {
        mapper.delete(model);
      }
    }
  }

  @Test
  public void testDelete() throws Exception {
    DataType model = null;
    DynamonitoMapper mapper = new DynamonitoMapper(getClient(), cacheAdaptor);
    try {
      model = DataType.buildModelWithRandomKey();
      mapper.save(model);
      assertEquals(model, mapper.load(DataType.class, model.getHashKey(), model.getRangeKey()));
      mapper.delete(model);
      DataType deletedModel = mapper.load(DataType.class, model.getHashKey(), model.getRangeKey());
      assertNull(deletedModel);
      String jsonFromCache = cacheAdaptor.get("DataType", model.getHashKey(), model.getRangeKey());
      assertNull(jsonFromCache);
    } finally {
      if (model != null) {
        mapper.delete(model);
      }
    }
  }

  @Test
  public void testUpdate() throws Exception {
    DataType originalModel = null;
    DynamonitoMapper mapper = new DynamonitoMapper(getClient(), cacheAdaptor);
    try {
      originalModel = DataType.buildModelWithRandomKey();
      mapper.save(originalModel);

      // Update the original model then save it. The cached model should reflect the change.
      originalModel.setDate(new Date(123L));
      originalModel.setString(null);
      originalModel.setBooleanPrimitive(!originalModel.isBooleanPrimitive());
      mapper.save(originalModel);
      DataType cachedModel = mapper.load(DataType.class, originalModel.getHashKey(), originalModel.getRangeKey());
      assertEquals(originalModel, cachedModel);
    } finally {
      if (originalModel != null) {
        mapper.delete(originalModel);
      }
    }
  }
}