/*
 * Copyright (C) 2001-2026 Food and Agriculture Organization of the
 * United Nations (FAO-UN), United Nations World Food Programme (WFP)
 * and United Nations Environment Programme (UNEP)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or (at
 * your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301, USA
 *
 * Contact: Jeroen Ticheler - FAO - Viale delle Terme di Caracalla 2,
 * Rome - Italy. email: geonetwork@osgeo.org
 */

package org.fao.geonet.schema.iso19139_rndt;

import org.fao.geonet.constants.Geonet;
import org.fao.geonet.domain.MetadataDraft;
import org.fao.geonet.kernel.search.EsSearchManager;
import org.fao.geonet.kernel.search.submission.DirectDeletionSubmitter;
import org.fao.geonet.listener.metadata.draft.DraftUtilities;
import org.fao.geonet.utils.Log;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Removes a working copy, and also its document in the search index.
 * <p>
 * Since GeoNetwork 4.4.13 (asynchronous indexing) DraftUtilities.removeDraft, called when a working copy is approved
 * and merged into the approved record, removes the draft from the index by UUID. A working copy has the UUID of its
 * approved record but its own document, "&lt;uuid&gt;-draft", which stayed in the index (editor board) after the
 * approval. The document is removed here by id, as GeoNetwork 4.4.12 did.
 * <p>
 * Registered in config-spring-geonetwork.xml as the primary DraftUtilities.
 */
public class RNDTDraftUtilities extends DraftUtilities {

    @Autowired
    private EsSearchManager searchManager;

    @Override
    public void removeDraft(MetadataDraft draft) {
        Integer id = draft.getId();
        super.removeDraft(draft);
        try {
            searchManager.deleteByQuery(String.format("+id:%d", id), DirectDeletionSubmitter.INSTANCE);
        } catch (Exception e) {
            Log.error(Geonet.DATA_MANAGER, "Couldn't remove the draft " + id + " from the index", e);
        }
    }
}
