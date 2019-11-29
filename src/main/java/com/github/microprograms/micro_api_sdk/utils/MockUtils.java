package com.github.microprograms.micro_api_sdk.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.github.microprograms.micro_api_sdk.model.PlainEntityDefinition;
import com.github.microprograms.micro_api_sdk.model.PlainEntityDefinition.PlainEntityMockConfig;
import com.github.microprograms.micro_api_sdk.model.PlainEntityRefDefinition;
import com.github.microprograms.micro_api_sdk.model.PlainEntityRefDefinition.PlainEntityRefItem.PlainEntityRefMockConfig;
import com.github.microprograms.micro_api_sdk.model.PlainFieldDefinition;
import com.github.microprograms.micro_api_sdk.model.PlainModelDefinition;
import com.github.microprograms.micro_oss_core.model.Entity;
import com.github.microprograms.micro_oss_core.model.Field;
import com.github.microprograms.micro_refs.model.Ref;
import com.github.microprograms.micro_refs.model.RefBuilder;

import org.apache.commons.lang3.StringUtils;

public class MockUtils {

    private static int random(int min, int max) {
        return min + new Random().nextInt(max - min + 1);
    }

    public static PlainModelMock mock(PlainModelDefinition modelDefinition, List<String> excludeModelNames,
            String javaPackageName) throws Exception {
        PlainModelMock modelMock = new PlainModelMock();
        for (PlainEntityDefinition x : modelDefinition.getEntityDefinitions()) {
            if (excludeModelNames.contains(x.getName())) {
                continue;
            }
            modelMock.add(mock(x, javaPackageName));
        }
        for (PlainEntityRefDefinition x : modelDefinition.getEntityRefDefinitions()) {
            modelMock.add(mock(x, javaPackageName, modelMock));
        }
        return modelMock;
    }

    private static PlainEntityMock mock(PlainEntityDefinition entityDefinition, String javaPackageName)
            throws ClassNotFoundException {
        Class<? extends Object> entityClass = ModelSdk.getEntityClass(entityDefinition.getName(), javaPackageName);
        PlainEntityMock entityMock = new PlainEntityMock(entityClass);
        PlainEntityMockConfig mockConfig = entityDefinition.getMock();
        int instanceCount = random(mockConfig.getMinInstanceCount(), mockConfig.getMaxInstanceCount());
        for (int i = 1; i <= instanceCount; i++) {
            JSONObject json = new JSONObject();
            for (PlainFieldDefinition x : entityDefinition.getFieldDefinitions()) {
                json.put(x.getName(), mock(x, i));
            }
            entityMock.add(JSON.toJavaObject(json, entityClass));
        }
        return entityMock;
    }

    private static String mock(PlainFieldDefinition fieldDefinition, int i) {
        String mock = fieldDefinition.getMock();
        if (null == mock) {
            return null;
        }

        if (StringUtils.isNotBlank(fieldDefinition.getName())) {
            mock = mock.replaceAll("\\$name", fieldDefinition.getName());
        }
        if (StringUtils.isNotBlank(fieldDefinition.getComment())) {
            mock = mock.replaceAll("\\$comment", fieldDefinition.getComment());
        }
        if (StringUtils.isNotBlank(fieldDefinition.getDescription())) {
            mock = mock.replaceAll("\\$description", fieldDefinition.getDescription());
        }
        if (fieldDefinition.getExample() != null) {
            mock = mock.replaceAll("\\$example", fieldDefinition.getExample().toString());
        }
        if (fieldDefinition.getDefaultValue() != null) {
            mock = mock.replaceAll("\\$defaultValue", fieldDefinition.getDefaultValue().toString());
        }

        return mock.replaceAll("\\$i", String.valueOf(i));
    }

    private static PlainEntityRefMock mock(PlainEntityRefDefinition entityRefDefinition, String javaPackageName,
            PlainModelMock modelMock) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException,
            NoSuchFieldException, SecurityException {
        Class<? extends Object> sourceClz = ModelSdk.getEntityClass(entityRefDefinition.getSource().getName(),
                javaPackageName);
        Class<? extends Object> targetClz = ModelSdk.getEntityClass(entityRefDefinition.getTarget().getName(),
                javaPackageName);
        EntityRefWorkspace entityRefWorkspace = EntityRefWorkspace.build(modelMock.get(sourceClz),
                modelMock.get(targetClz), entityRefDefinition.getSource().getMock(),
                entityRefDefinition.getTarget().getMock());
        return entityRefWorkspace.mock();
    }

    public static class PlainModelMock {
        private List<PlainEntityMock> entityMocks = new ArrayList<>();
        private List<PlainEntityRefMock> entityRefMocks = new ArrayList<>();

        public void add(PlainEntityMock entityMock) {
            entityMocks.add(entityMock);
        }

        public void add(PlainEntityRefMock entityRefMock) {
            entityRefMocks.add(entityRefMock);
        }

        public PlainEntityMock get(Class<? extends Object> clz) {
            for (PlainEntityMock x : entityMocks) {
                if (clz == x.getClz()) {
                    return x;
                }
            }
            return null;
        }

        public PlainEntityRefMock getRef(Class<? extends Object> sourceClz, Class<? extends Object> targetClz) {
            for (PlainEntityRefMock x : entityRefMocks) {
                if (sourceClz == x.getSourceClz() && targetClz == x.getTargetClz()) {
                    return x;
                }
            }
            return null;
        }

        public List<PlainEntityMock> getEntityMocks() {
            return entityMocks;
        }

        public void setEntityMocks(List<PlainEntityMock> entityMocks) {
            this.entityMocks = entityMocks;
        }

        public List<PlainEntityRefMock> getEntityRefMocks() {
            return entityRefMocks;
        }

        public void setEntityRefMocks(List<PlainEntityRefMock> entityRefMocks) {
            this.entityRefMocks = entityRefMocks;
        }
    }

    public static class PlainEntityMock {
        private Class<? extends Object> clz;
        private List<Object> instances = new ArrayList<>();

        public PlainEntityMock(Class<? extends Object> clz) {
            this.clz = clz;
        }

        public void add(Object instance) {
            instances.add(instance);
        }

        public Class<? extends Object> getClz() {
            return clz;
        }

        public void setClz(Class<? extends Object> clz) {
            this.clz = clz;
        }

        public List<Object> getInstances() {
            return instances;
        }

        public void setInstances(List<Object> instances) {
            this.instances = instances;
        }
    }

    public static class PlainEntityRefMock {
        private Class<? extends Object> sourceClz;
        private Class<? extends Object> targetClz;
        private List<Ref> refs = new ArrayList<>();

        public PlainEntityRefMock(Class<? extends Object> sourceClz, Class<? extends Object> targetClz) {
            this.sourceClz = sourceClz;
            this.targetClz = targetClz;
        }

        public void add(Ref ref) {
            refs.add(ref);
        }

        public Class<? extends Object> getSourceClz() {
            return sourceClz;
        }

        public void setSourceClz(Class<? extends Object> sourceClz) {
            this.sourceClz = sourceClz;
        }

        public Class<? extends Object> getTargetClz() {
            return targetClz;
        }

        public void setTargetClz(Class<? extends Object> targetClz) {
            this.targetClz = targetClz;
        }

        public List<Ref> getRefs() {
            return refs;
        }

        public void setRefs(List<Ref> refs) {
            this.refs = refs;
        }
    }

    public static class EntityRefWorkspace {
        private Class<? extends Object> sourceClz;
        private Class<? extends Object> targetClz;
        private List<EntityInstance> sourceEntityInstances;
        private List<EntityInstance> targetEntityInstances;

        public static EntityRefWorkspace build(PlainEntityMock source, PlainEntityMock target,
                PlainEntityRefMockConfig sourceRefConfig, PlainEntityRefMockConfig targetRefConfig) {
            return new EntityRefWorkspace(source.getClz(), target.getClz(), _build(source, sourceRefConfig),
                    _build(target, targetRefConfig));
        }

        private static List<EntityInstance> _build(PlainEntityMock entity, PlainEntityRefMockConfig refConfig) {
            List<EntityInstance> entityInstances = new ArrayList<>();
            for (Object instance : entity.getInstances()) {
                int repeatPerInstance = random(refConfig.getMinRepeatPerInstance(),
                        refConfig.getMaxRepeatPerInstance());
                entityInstances.add(new EntityInstance(instance, repeatPerInstance));
            }
            return entityInstances;
        }

        public EntityRefWorkspace(Class<? extends Object> sourceClz, Class<? extends Object> targetClz,
                List<EntityInstance> sourceEntityInstances, List<EntityInstance> targetEntityInstances) {
            this.sourceClz = sourceClz;
            this.targetClz = targetClz;
            this.sourceEntityInstances = sourceEntityInstances;
            this.targetEntityInstances = targetEntityInstances;
        }

        public PlainEntityRefMock mock()
                throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
            PlainEntityRefMock refMock = new PlainEntityRefMock(sourceClz, targetClz);
            Collections.shuffle(sourceEntityInstances);
            Collections.shuffle(targetEntityInstances);
            for (EntityInstance source : sourceEntityInstances) {
                String sourceId = _getId(source.getInstance());
                for (int i = 0; i < source.getRepeat(); i++) {
                    Object target = _pop(targetEntityInstances);
                    if (null == target) {
                        break;
                    }
                    String targetId = _getId(target);
                    refMock.add(new RefBuilder().location(sourceClz, sourceId).location(targetClz, targetId).build());
                }
            }
            return refMock;
        }

        private static String _getId(Object instance)
                throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
            return (String) instance.getClass().getField("id").get(instance);
        }

        private static Object _pop(List<EntityInstance> entityInstances) {
            if (entityInstances.isEmpty()) {
                return null;
            }

            EntityInstance entityInstance = entityInstances.get(0);
            if (entityInstance.getRepeat() <= 0) {
                entityInstances.remove(entityInstance);
                return _pop(entityInstances);
            }

            entityInstance.setRepeat(entityInstance.getRepeat() - 1);
            return entityInstance.getInstance();
        }

        public Class<? extends Object> getSourceClz() {
            return sourceClz;
        }

        public void setSourceClz(Class<? extends Object> sourceClz) {
            this.sourceClz = sourceClz;
        }

        public Class<? extends Object> getTargetClz() {
            return targetClz;
        }

        public void setTargetClz(Class<? extends Object> targetClz) {
            this.targetClz = targetClz;
        }

        public List<EntityInstance> getSourceEntityInstances() {
            return sourceEntityInstances;
        }

        public void setSourceEntityInstances(List<EntityInstance> sourceEntityInstances) {
            this.sourceEntityInstances = sourceEntityInstances;
        }

        public List<EntityInstance> getTargetEntityInstances() {
            return targetEntityInstances;
        }

        public void setTargetEntityInstances(List<EntityInstance> targetEntityInstances) {
            this.targetEntityInstances = targetEntityInstances;
        }

        public static class EntityInstance {
            private Object instance;
            private int repeat;

            public EntityInstance(Object instance, int repeat) {
                this.instance = instance;
                this.repeat = repeat;
            }

            public Object getInstance() {
                return instance;
            }

            public void setInstance(Object instance) {
                this.instance = instance;
            }

            public int getRepeat() {
                return repeat;
            }

            public void setRepeat(int repeat) {
                this.repeat = repeat;
            }
        }
    }

}
