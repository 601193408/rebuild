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

package com.rebuild.web.base.entity;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.rebuild.server.helper.manager.FormsManager;
import com.rebuild.server.helper.manager.ViewAddonsManager;
import com.rebuild.server.helper.manager.value.DefaultValueManager;
import com.rebuild.server.metadata.MetadataHelper;
import com.rebuild.web.BaseEntityControll;

import cn.devezhao.commons.web.ServletUtils;
import cn.devezhao.persist4j.Entity;
import cn.devezhao.persist4j.engine.ID;

/**
 * 表单/视图
 * 
 * @author zhaofang123@gmail.com
 * @since 08/22/2018
 */
@Controller
@RequestMapping("/app/{entity}/")
public class GeneralModelControll extends BaseEntityControll {

	@RequestMapping("view/{id}")
	public ModelAndView pageView(@PathVariable String entity, @PathVariable String id,
			HttpServletRequest request) throws IOException {
		ID user = getRequestUser(request);
		Entity thatEntity = MetadataHelper.getEntity(entity);
		
		ID record = ID.valueOf(id);
		ModelAndView mv = null;
		if (thatEntity.getMasterEntity() != null) {
			mv = createModelAndView("/general-entity/slave-view.jsp", record, user);
		} else {
			mv = createModelAndView("/general-entity/record-view.jsp", record, user);
			
			JSON vtab = ViewAddonsManager.getViewTab(entity, user);
			mv.getModel().put("ViewTabs", vtab);
			JSON vadd = ViewAddonsManager.getViewAdd(entity, user);
			mv.getModel().put("ViewAdds", vadd);
		}
		mv.getModel().put("id", record);
		
		return mv;
	}
	
	@RequestMapping("form-model")
	public void entityForm(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		ID record = getIdParameter(request, "id");  // New or Update
		
		JSON initialVal = null;
		if (record == null) {
			initialVal = ServletUtils.getRequestJson(request);
			if (initialVal != null) {
				// 创建明细实体必须指定主实体，以便验证权限
				String master = ((JSONObject) initialVal).getString(DefaultValueManager.DV_MASTER);
				if (ID.isId(master)) {
					FormsManager.setCurrentMasterId(ID.valueOf(master));
				}
			}
		}
		
		JSON model = FormsManager.getFormModel(entity, user, record);
		// 填充前端设定的初始值
		if (record == null && initialVal != null) {
			DefaultValueManager.setFieldsValue(MetadataHelper.getEntity(entity), model, initialVal);
		}
		writeSuccess(response, model);
	}
	
	@RequestMapping("view-model")
	public void entityView(@PathVariable String entity,
			HttpServletRequest request, HttpServletResponse response) throws IOException {
		ID user = getRequestUser(request);
		ID record = getIdParameterNotNull(request, "id");
		JSON modal = FormsManager.getViewModel(entity, user, record);
		writeSuccess(response, modal);
	}
}
