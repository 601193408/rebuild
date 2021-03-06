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

package com.rebuild.server.helper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.rebuild.server.Application;
import com.rebuild.server.metadata.EntityHelper;
import com.rebuild.server.service.bizz.UserService;

import cn.devezhao.persist4j.Record;
import cn.devezhao.persist4j.engine.ID;

/**
 * 全局系统配置
 * 
 * @author devezhao
 * @since 10/14/2018
 */
public class SystemConfig {
	
	private static final Log LOG = LogFactory.getLog(SystemConfig.class);
	
	/**
	 * 临时目录/文件
	 * 
	 * @param file
	 * @return
	 */
	public static File getFileOfTemp(String file) {
		String tmp = get(ConfigItem.TempDirectory);
		File tmpFile = null;
		if (tmp != null) {
			tmpFile = new File(tmp);
			if (!tmpFile.exists()) {
				LOG.warn("TempDirectory not exists : " + tmp);
				tmpFile = FileUtils.getTempDirectory();
			}
		} else {
			tmpFile = FileUtils.getTempDirectory();
		}
		return new File(tmpFile, file);
	}
	
	/**
	 * 云存储地址
	 * 
	 * @return
	 */
	public static String getStorageUrl() {
		String account[] = getStorageAccount();
		return account == null ? null : account[3];
	}
	
	/**
	 * 云存储账号
	 * 
	 * @return returns [StorageApiKey, StorageApiSecret, StorageBucket, StorageURL]
	 */
	public static String[] getStorageAccount() {
		return getsNoUnset(
				ConfigItem.StorageApiKey, ConfigItem.StorageApiSecret, ConfigItem.StorageBucket, ConfigItem.StorageURL);
	}
	
	/**
	 * 缓存账号
	 * 
	 * @return returns [CacheHost, CachePort, CachePassword]
	 */
	public static String[] getCacheAccount() {
		return getsNoUnset(
				ConfigItem.CacheHost, ConfigItem.CachePort, ConfigItem.CachePassword);
	}
	
	/**
	 * 邮件账号
	 * 
	 * @return returns [MailUser, MailPassword, MailAddr, MailName]
	 */
	public static String[] getMailAccount() {
		return getsNoUnset(
				ConfigItem.MailUser, ConfigItem.MailPassword, ConfigItem.MailAddr, ConfigItem.MailName);
	}
	
	/**
	 * 短信账号
	 * 
	 * @return returns [SmsUser, SmsPassword, SmsSign]
	 */
	public static String[] getSmsAccount() {
		return getsNoUnset(
				ConfigItem.SmsUser, ConfigItem.SmsPassword, ConfigItem.SmsSign);
	}
	
	/**
	 * 获取多个，任意一个为空都返回 null
	 * 
	 * @param items
	 * @return
	 */
	static String[] getsNoUnset(ConfigItem... items) {
		List<String> list = new ArrayList<>();
		for (ConfigItem item : items) {
			String v = get(item);
			if (v == null) {
				return null;
			}
			list.add(v);
		}
		return list.toArray(new String[list.size()]);
	}
	
	// --
	
	/**
	 * @param name
	 * @return
	 */
	public static String get(ConfigItem name) {
		return get(name, false);
	}
	
	/**
	 * @param name
	 * @param reload
	 * @return
	 */
	public static String get(ConfigItem name, boolean reload) {
		final String key = name.name();
		String s = Application.getCommonCache().get(key);
		if (s != null && reload == false) {
			return s;
		}
		
		Object[] value = Application.createQueryNoFilter(
				"select value from SystemConfig where item = ?")
				.setParameter(1, name.name())
				.unique();
		s = value == null ? null : StringUtils.defaultIfBlank((String) value[0], null);
		
		// 从配置文件加载
		if (s == null) {
			s = Application.getBean(AesPreferencesConfigurer.class).getItem(key);
		}
		
		if (s == null) {
			Application.getCommonCache().evict(key);
		} else {
			Application.getCommonCache().put(key, s);
		}
		return s;
	}
	
	/**
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static long getLong(ConfigItem name, long defaultValue) {
		String s = get(name);
		return s == null ? defaultValue : NumberUtils.toLong(s);
	}
	
	/**
	 * @param name
	 * @param defaultValue
	 * @return
	 */
	public static boolean getBool(ConfigItem name, boolean defaultValue) {
		String s = get(name);
		return s == null ? defaultValue : BooleanUtils.toBoolean(s);
	}
	
	/**
	 * @param name
	 * @param value
	 * @return
	 */
	public static void set(ConfigItem name, Object value) {
		Object[] exists = Application.createQueryNoFilter(
				"select configId from SystemConfig where item = ?")
				.setParameter(1, name.name())
				.unique();
		
		Record record = null;
		if (exists == null) {
			record = EntityHelper.forNew(EntityHelper.SystemConfig, UserService.SYSTEM_USER);
			record.setString("item", name.name());
		} else {
			record = EntityHelper.forUpdate((ID) exists[0], UserService.SYSTEM_USER);
		}
		record.setString("value", value.toString());
		
		Application.getCommonService().createOrUpdate(record);
		get(name, true);
	}
}
