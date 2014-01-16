package edu.zju.bme.openehr.hibernate.dao;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.openehr.am.archetype.Archetype;
import org.openehr.am.parser.ContentObject;
import org.openehr.am.parser.DADLParser;
import org.openehr.rm.binding.DADLBinding;
import org.openehr.rm.common.archetyped.Locatable;
import org.openehr.rm.support.identification.HierObjectID;
import org.openehr.rm.support.identification.UIDBasedID;
import org.springframework.stereotype.Component;

import se.acode.openehr.parser.ADLParser;
import edu.zju.bme.openehr.hibernate.model.CoarseNodePathIndex;
import edu.zju.bme.snippet.java.Reflector;

@Component("RMORMPersistenceDao")
public class RMORMPersistenceDaoImpl implements RMORMPersistenceDao {

	private Logger logger = Logger.getLogger(RMORMPersistenceDaoImpl.class.getName());

	@Resource(name="sessionFactory")
	private SessionFactory sessionFactory;

    @Override
	public int insert(List<String> dadls, List<String> adls) {

		logger.info("insert");

		try {
			
			Map<String, Archetype> archetypes = new HashMap<>();
			for (String adl : adls) {
				ADLParser parser = new ADLParser(adl);
				Archetype archetype = parser.parse();
				archetypes.put(archetype.getArchetypeId().getValue(), archetype); 
			}

			Session s = sessionFactory.getCurrentSession();
			DADLBinding binding = new DADLBinding();

			for (String dadl : dadls) {
				logger.info(dadl);
				InputStream is = new ByteArrayInputStream(dadl.getBytes("UTF-8"));
				DADLParser parser = new DADLParser(is);
				ContentObject contentObj = parser.parse();
				Object archetypeInstance = binding.bind(contentObj);
				
				if (archetypeInstance instanceof Locatable) {
					Locatable loc = (Locatable) archetypeInstance;
					
					UIDBasedID uid = loc.getUid();
					if (loc.getUid() == null) {
						uid = new HierObjectID(UUID.randomUUID().toString());
						loc.setUid(uid);
					} else if (uid.getValue() == null || uid.getValue().isEmpty()) {
						uid.setValue(UUID.randomUUID().toString());
					}					
					
					s.save(loc);
					
					Query q = s.createQuery("delete from CoarseNodePathIndex as c where c.referenceId = :referenceId");
					q.setParameter("referenceId", loc.getMappingId());
					q.executeUpdate();
					
					Archetype archetype = archetypes.get(loc.getArchetypeNodeId());
					Set<String> pathSet = archetype.getPathNodeMap().keySet();
					for (String path : pathSet) {
						persistNodePOJOFields(path, loc, loc.getMappingId());
					}
				}
			}
			
		} catch (Exception e) {
			logger.error(e);
			return -2;
		}

		return 0;

	}
    
    @Override
	public int delete(String aql) {

		return delete(aql, null);

	}

	public int delete(String aql, Map<String, Object> parameters) {

		return executeUpdate(aql, parameters);

	}

	protected int executeUpdate(String aql, Map<String, Object> parameters) {

		logger.info("executeUpdate");

		logger.info(aql);

		try {

			Session s = sessionFactory.getCurrentSession();

			Query q = s.createQuery(aql);
			passParameters(q, parameters);
			int ret = q.executeUpdate();

			logger.info(ret);

			return ret;
		} catch (Exception e) {
			logger.error(e);
			return -2;
		}

	}
    
    @Override
    public List<String> selectPersonByObjectUids(List<String> objectUids) {

		logger.info("selectPersonByObjectUids");

		try {
			
			String queryString = "from Person as c where c.uid.value in :objectUid";

			Session s = sessionFactory.getCurrentSession();
			
			Query query = s.createQuery(queryString);
			query.setParameterList("objectUid", objectUids);
			
			@SuppressWarnings("rawtypes")
			List results = query.list();

			List<String> dadlResults = new ArrayList<String>();
			for (Object arr : results) {
				if (arr.getClass().isArray()) {
					for (int i = 0; i < Array.getLength(arr); i++) {
						generateReturnDADL(Array.get(arr, i), dadlResults);
					}
				} else {
					generateReturnDADL(arr, dadlResults);
				}
			}

			return dadlResults;
			
		} catch (Exception e) {
			logger.error(e);
			return null;
		}
    	
    }
//    
//    @Override
//    public List<CoarseNodePathEntity> selectCoarseNodePathByIds(List<Integer> ids) {
//
//		logger.info("selectCoarseNodePathByIds");
//
//		try {
//			
//			String queryString = "from CoarseNodePathEntity as c where c.id in :id";
//
//			Session s = sessionFactory.getCurrentSession();
//			
//			Query query = s.createQuery(queryString);
//			query.setParameterList("id", ids);
//			
//			@SuppressWarnings("unchecked")
//			List<CoarseNodePathEntity> listCoarseNodePath = query.list();		
//			
//			return listCoarseNodePath;
//			
//		} catch (Exception e) {
//			logger.error(e);
//			return null;
//		}
//    	
//    }
    
//    @Override
//    public List<CoarseNodePathEntity> selectCoarseNodePathByPathValues(Map<String, String> pathValues) {
//
//		logger.info("selectCoarseNodePathByPathValues");
//
//		try {
//			
//			String queryString = "from CoarseNodePathIndex as c where ";
//
//			Session s = sessionFactory.getCurrentSession();
//			
//			for (int i = 0; i < pathValues.size(); i++) {				
//				String conditionString ="(c.path = :path" + i + " and c.valueString = :value" + i + ")";
//				queryString += conditionString;
//				if (i < pathValues.size() - 1) {
//					queryString += " or ";						
//				}			
//			}
//
//			Query query = s.createQuery(queryString);
//			int i = 0;
//			for (String key : pathValues.keySet()) {
//				String value  = pathValues.get(key);				
//				query.setParameter("path" + i, key);
//				query.setParameter("value" + i, value);			
//				i++;
//			}
//			
//			@SuppressWarnings("unchecked")
//			List<CoarseNodePathIndex> listCoarseNodePathIndex = query.list();
//			
//			List<Integer> coarseNodePathEntityIds = new ArrayList<>();
//			for (CoarseNodePathIndex coarseNodePathIndex : listCoarseNodePathIndex) {
//				if (coarseNodePathEntityIds.contains(coarseNodePathIndex.getReferenceId())) {
//					continue;
//				}
//				coarseNodePathEntityIds.add(coarseNodePathIndex.getReferenceId());
//			}
//
//			List<CoarseNodePathEntity> listCoarseNodePathEntity = selectCoarseNodePathByIds(coarseNodePathEntityIds);
//			return listCoarseNodePathEntity;
//			
//		} catch (Exception e) {
//			logger.error(e);
//			return null;
//		}
//    	
//    }

//    @Override
//	public List<CoarseNodePathEntity> selectCoarseNodePathByPathValues(List<String> paths, List<String> values) {
//
//		logger.info("selectCoarseNodePathByPathValues");
//
//		try {
//			
//			String queryString = "from CoarseNodePathIndex as c where ";
//
//			Session s = sessionFactory.getCurrentSession();
//			
//			for (int i = 0; i < paths.size(); i++) {				
//				String conditionString ="(c.path = :path" + i + " and c.valueString = :value" + i + ")";
//				queryString += conditionString;
//				if (i < paths.size() - 1) {
//					queryString += " or ";						
//				}			
//			}
//
//			Query query = s.createQuery(queryString);
//			for (int i = 0; i < paths.size(); i++) {			
//				query.setParameter("path" + i, paths.get(i));
//				query.setParameter("value" + i, values.get(i));	
//				
//			}
//			
//			@SuppressWarnings("unchecked")
//			List<CoarseNodePathIndex> listCoarseNodePathIndex = query.list();
//			
//			List<Integer> coarseNodePathEntityIds = new ArrayList<>();
//			for (CoarseNodePathIndex coarseNodePathIndex : listCoarseNodePathIndex) {
//				if (coarseNodePathEntityIds.contains(coarseNodePathIndex.getReferenceId())) {
//					continue;
//				}
//				coarseNodePathEntityIds.add(coarseNodePathIndex.getReferenceId());
//			}
//
//			List<CoarseNodePathEntity> listCoarseNodePathEntity = selectCoarseNodePathByIds(coarseNodePathEntityIds);
//			return listCoarseNodePathEntity;
//			
//		} catch (Exception e) {
//			logger.error(e);
//			return null;
//		}
//    	
//    }

	protected void passParameters(Query q, Map<String, Object> parameters) {

		if (parameters != null) {
			for (String paraName : parameters.keySet()) {
				q.setParameter(paraName, parameters.get(paraName));
			}
		}

	}
	
	protected void persistNodePOJOFields(String nodePath, Locatable loc, int id) 
			throws IllegalArgumentException, IllegalAccessException {
		
		Object obj = loc.itemAtPath(nodePath);
		if (obj == null) {
			return;
		}
		
		Session s = sessionFactory.getCurrentSession();
		
		Iterable<Field> fields = Reflector.INSTANCE.getFieldsUpTo(obj.getClass(), null);
		for (Field field : fields) {
			field.setAccessible(true);
			String fieldPath = nodePath + "/" + field.getName();
			CoarseNodePathIndex coarseNodePathIndex = new CoarseNodePathIndex();
			coarseNodePathIndex.setReferenceId(id);
			coarseNodePathIndex.setPath(fieldPath);
			if (field.getType() == String.class || 
					field.getType() == Integer.class || 
					field.getType() == Double.class || 
					field.getType() == Date.class || 
					field.getType() == Boolean.class) {
				if (field.get(obj) == null) {
					coarseNodePathIndex.setValueString(null);
				} else {
					coarseNodePathIndex.setValueString(field.get(obj).toString());
				}		
			} else {
				continue;
			}
			s.save(coarseNodePathIndex);			
		}
		
	}

	protected void generateReturnDADL(Object obj, List<String> dadlResults)
			throws Exception {

		if (obj instanceof Locatable) {
			DADLBinding binding = new DADLBinding();
			Locatable loc = (Locatable) obj;
			String str = binding.toDADLString(loc);
			if (!dadlResults.contains(str)) {
				logger.info(str);
				dadlResults.add(str);

				for (Object associatedObject : loc.getAssociatedObjects()
						.values()) {
					generateReturnDADL(associatedObject, dadlResults);
				}
			}
		}

	}

}