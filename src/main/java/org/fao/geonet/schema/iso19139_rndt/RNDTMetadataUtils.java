package org.fao.geonet.schema.iso19139_rndt;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import jeeves.server.context.ServiceContext;
import org.fao.geonet.NodeInfo;
import org.fao.geonet.domain.MetadataDraft;
import org.fao.geonet.kernel.datamanager.draft.DraftMetadataUtils;
import org.fao.geonet.kernel.search.EsSearchManager;
import org.fao.geonet.repository.MetadataDraftRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Metadata utils of the RNDT profile: the working copies of GeoNetwork 4 with a schema that reads the UUID from the
 * record (readwriteUuid=true in schema-ident.xml), and a popularity counter that does not overwrite the record.
 * <p>
 * When a record is saved, BaseMetadataManager.updateMetadata extracts the UUID from the XML (only for schemas with
 * readwriteUuid) and BaseMetadataUtils.checkMetadataWithSameUuidExist looks for a published record with that UUID
 * and a different id. A working copy (MetadataDraft) has the UUID of its published record and its own id, so saving
 * it failed with "Another record exist with UUID ...". The core schemas are not affected because they do not read
 * the UUID from the record, so the check never runs for them.
 * <p>
 * Registered in config-spring-geonetwork.xml as the primary IMetadataUtils, next to the DraftMetadataUtils of the
 * core. Both run BaseMetadataUtils.init (metadataIndexer.setMetadataUtils): whichever the indexer gets, it behaves
 * the same, since the indexer never calls these methods.
 */
public class RNDTMetadataUtils extends DraftMetadataUtils {

    @Autowired
    private MetadataDraftRepository metadataDraftRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private EsSearchManager searchManager;

    @Autowired
    private NodeInfo nodeInfo;

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Accepts a working copy that keeps the UUID of its published record; any other case goes to the core check,
     * so a working copy whose UUID is changed to that of another record is still refused.
     */
    @Override
    public boolean checkMetadataWithSameUuidExist(String uuid, int id) {
        if (uuid != null) {
            MetadataDraft draft = metadataDraftRepository.findById(id).orElse(null);
            if (draft != null && uuid.equals(draft.getUuid())) {
                return false;
            }
        }
        return super.checkMetadataWithSameUuidExist(uuid, id);
    }

    /**
     * Adds one to the popularity of a record by updating that column only.
     * <p>
     * The core (BaseMetadataUtils.increasePopularity, called every time the page of a record is opened) loads the
     * whole record, increments the counter and writes the whole record back, XML and change date included. When the
     * page is opened while the record is being saved, the counter writes back the version it read before the save
     * and the edit is lost in the database, while the search index keeps it (only the _popularity field is updated
     * there): a lost update, seen with GeoNetwork 4.4.12 when the first save after a start takes several seconds.
     * Same result as the core otherwise: popularity + 1 in the table and in the index, nothing in read-only mode.
     */
    @Override
    public void increasePopularity(ServiceContext context, String id) {
        if (nodeInfo.isReadOnly()) {
            return;
        }
        final int metadataId = Integer.parseInt(id);
        Object[] uuidAndPopularity = new TransactionTemplate(transactionManager).execute(status -> {
            int updated = entityManager
                .createNativeQuery("UPDATE metadata SET popularity = popularity + 1 WHERE id = ?1")
                .setParameter(1, metadataId)
                .executeUpdate();
            if (updated == 0) {
                return null;
            }
            return (Object[]) entityManager
                .createNativeQuery("SELECT uuid, popularity FROM metadata WHERE id = ?1")
                .setParameter(1, metadataId)
                .getSingleResult();
        });
        if (uuidAndPopularity != null) {
            searchManager.updateFieldAsynch((String) uuidAndPopularity[0], "_popularity",
                ((Number) uuidAndPopularity[1]).intValue());
        }
    }
}
