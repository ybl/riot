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
 * Portions created by the Initial Developer are Copyright (C) 2008
 * the Initial Developer. All Rights Reserved.
 * 
 * Contributor(s):
 *   Carsten Woelk [cwoelk at neteye dot de]
 * 
 * ***** END LICENSE BLOCK ***** */
package org.riotfamily.pages.view;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.riotfamily.common.web.servlet.PathCompleter;
import org.riotfamily.common.web.view.MacroHelperFactory;
import org.riotfamily.pages.dao.PageDao;
import org.riotfamily.pages.mapping.PageResolver;

/**
 * @author Carsten Woelk [cwoelk at neteye dot de]
 * @since 7.0
 */
public class PagesMacroHelperFactory implements MacroHelperFactory {

	private PageDao pageDao;

	private PageResolver pageResolver;

	private PathCompleter pathCompleter;
	
	public PagesMacroHelperFactory(PageDao pageDao, PageResolver pageResolver,
			PathCompleter pathCompleter) {
		
		this.pageDao = pageDao;
		this.pageResolver = pageResolver;
		this.pathCompleter = pathCompleter;
	}

	public Object createMacroHelper(HttpServletRequest request,
			HttpServletResponse response) {
		
		return new PagesMacroHelper(pageDao, pageResolver,
				pathCompleter, request);
	}
	
}
