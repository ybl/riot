/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Riot.
 *
 * The Initial Developer of the Original Code is
 * Neteye GmbH.
 * Portions created by the Initial Developer are Copyright (C) 2007
 * the Initial Developer. All Rights Reserved.
 *
 * Contributor(s):
 *   Felix Gnass [fgnass at neteye dot de]
 *
 * ***** END LICENSE BLOCK ***** */
package org.riotfamily.pages.setup;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.riotfamily.pages.dao.PageDao;
import org.riotfamily.pages.dao.PageDefinition;
import org.riotfamily.pages.model.PageNode;
import org.riotfamily.pages.model.Site;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Felix Gnass [fgnass at neteye dot de]
 * @since 6.5
 */
public class PageSetupBean implements InitializingBean, ApplicationContextAware {

	private List sites;

	private List definitions;

	private PageDao pageDao;

	private PlatformTransactionManager transactionManager;
	
	private ApplicationContext applicationContext;

	public void setPageDao(PageDao pageDao) {
		this.pageDao = pageDao;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setSites(List sites) {
		this.sites = sites;
	}

	public void setDefinitions(List definitions) {
		this.definitions = definitions;
	}

	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}
	
	public void afterPropertiesSet() throws Exception {
		if (transactionManager == null) {
			transactionManager = (PlatformTransactionManager) 
					BeanFactoryUtils.beanOfTypeIncludingAncestors(
					applicationContext, PlatformTransactionManager.class);
		}
		if (pageDao == null) {
			pageDao = (PageDao)	BeanFactoryUtils.beanOfTypeIncludingAncestors(
					applicationContext, PageDao.class);
		}
		new TransactionTemplate(transactionManager).execute(new TransactionCallbackWithoutResult() {
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				createNodes();
			}
		});
	}

	protected void createNodes() {
		if (pageDao.listSites().isEmpty()) {
			createSites();
			PageNode rootNode = pageDao.getRootNode();
			Iterator it = definitions.iterator();
			while (it.hasNext()) {
				PageDefinition definition = (PageDefinition) it.next();
				 definition.createNode(rootNode, sites, pageDao);
			}
			pageDao.updateNode(rootNode);
		}
	}
	
	protected void createSites() {
		if (sites == null || sites.isEmpty()) {
			Site site = new Site();
			site.setLocale(Locale.ENGLISH);
			site.setEnabled(true);
			sites = Collections.singletonList(site);
		}
		Iterator it = sites.iterator();
		while (it.hasNext()) {
			Site site = (Site) it.next();
			pageDao.saveSite(site);
		}
	}


}
