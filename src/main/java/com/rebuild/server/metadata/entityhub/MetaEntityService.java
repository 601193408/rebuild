/*
rebuild - Building your business-systems freely.
Copyright (C) 2018 devezhao <zhaofang123@gmail.com>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
*/

package com.rebuild.server.metadata.entityhub;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.server.service.BaseService;

import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.PersistManagerFactory;
import cn.devezhao.persist4j.engine.ID;

/**
 * @author zhaofang123@gmail.com
 * @since 08/03/2018
 */
public class MetaEntityService extends BaseService {
	
	private static final Log LOG = LogFactory.getLog(MetaEntityService.class);

	public MetaEntityService(PersistManagerFactory aPMFactory) {
		super(aPMFactory);
	}
	
	@Override
	public int delete(ID recordId) {
		Object[] entityRecord = getPMFactory().createQuery(
				"select entityName from MetaEntity where entityId = ?")
				.setParameter(1, recordId)
				.unique();
		Entity entity = MetadataHelper.getEntity((String) entityRecord[0]);
		
		// 删除此实体的相关配置记录
		String whoUsed[] = new String[] {
				"MetaField", "PickList", "LayoutConfig", "FilterConfig", "ViewAddonsConfig", "ShareAccess", "ChartConfig"
		};
		int del = 0;
		for (String who : whoUsed) {
			Entity whoEntity = MetadataHelper.getEntity(who);
			if (!whoEntity.containsField("belongEntity")) {
				continue;
			}
			
			String sql = String.format("select %s from %s where belongEntity = '%s'", 
					whoEntity.getPrimaryField().getName(), whoEntity.getName(), entity.getName());
			Object[][] usedArray = getPMFactory().createQuery(sql).array();
			for (Object[] used : usedArray) {
				del += super.delete((ID) used[0]);
			}
			LOG.warn("deleted configuration [ " + entity.getName() + " ] from [ " + who + " ] : " + usedArray.length);
		}
		
		del += super.delete(recordId);
		return del;
	}
}
