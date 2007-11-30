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
 * Portions created by the Initial Developer are Copyright (C) 2006
 * the Initial Developer. All Rights Reserved.
 * 
 * Contributor(s):
 *   Felix Gnass [fgnass at neteye dot de]
 * 
 * ***** END LICENSE BLOCK ***** */
package org.riotfamily.riot.resource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.SocketException;
import java.util.Iterator;
import java.util.List;

import javax.activation.FileTypeMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.riotfamily.cachius.spring.AbstractCacheableController;
import org.riotfamily.cachius.spring.Compressable;
import org.riotfamily.common.web.util.ServletUtils;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.LastModified;

/**
 * Controller that serves an internal resource.
 */
public class AbstractResourceController extends AbstractCacheableController
		implements LastModified, Compressable {

	private Log log = LogFactory.getLog(AbstractResourceController.class);
	
	private FileTypeMap fileTypeMap = FileTypeMap.getDefaultFileTypeMap();
	
    private List mappings;
    
    private List filters;
    
	private long lastModified = System.currentTimeMillis();
	
	private boolean checkForModifications = false;
	
	private String pathAttribute;
	
	private String pathParameter;
	
		        
	/**
	 * Sets the name of the request attribute that will contain the 
	 * resource path. 
	 */
	public void setPathAttribute(String pathAttribute) {
		this.pathAttribute = pathAttribute;
	}
	
	/**
	 * Sets the name of the request parameter that will contain the 
	 * resource path.
	 */
	public void setPathParameter(String pathParameter) {
		this.pathParameter = pathParameter;
	}
	
    public void setFileTypeMap(FileTypeMap fileTypeMap) {
		this.fileTypeMap = fileTypeMap;
	}

	public void setMappings(List resourceMappings) {
		this.mappings = resourceMappings;
	}

	public void setFilters(List filters) {
		this.filters = filters;
	}

	/**
	 * Sets whether the controller check for file modifications.
	 */
	public void setCheckForModifications(boolean checkForModifications) {
		this.checkForModifications = checkForModifications;
	}
	
	protected Resource lookupResource(String path) throws IOException {
		Iterator it = mappings.iterator();
    	while (it.hasNext()) {
    		ResourceMapping mapping = (ResourceMapping) it.next();
			Resource res = mapping.getResource(path);
			if (res != null) {
				return res;
			}
    	}
    	return null;
	}
	
	protected String getContentType(Resource resource) {
		if (resource == null) {
			return null;
		}
		return fileTypeMap.getContentType(resource.getFilename());
	}
	
	protected String getResourcePath(HttpServletRequest request) {
		if (pathAttribute != null) {
    		return "/" + request.getAttribute(pathAttribute); 
    	}
		else if (pathParameter != null) {
			return "/" + request.getParameter(pathParameter);
		}
   		return request.getPathInfo();
	}
	
	public long getLastModified(HttpServletRequest request) {
		if (checkForModifications) {
			String path = getResourcePath(request);
			long mtime = getLastModified(path);
			return mtime >= 0 ? mtime : lastModified;
		}
		return lastModified;
	}
	
	protected long getLastModified(String path) {
		try {
			Resource res = lookupResource(path);
			if (res != null) {
				return res.getFile().lastModified();
			}
		}
		catch (IOException e) {
		}
		return -1;
	}
	
	public long getTimeToLive() {
		return checkForModifications ? 0 : CACHE_ETERNALLY;
	}
	
	protected String getCacheKeyInternal(HttpServletRequest request) {
		StringBuffer key = new StringBuffer();
		key.append(getResourcePath(request));
		String lang = request.getParameter("lang");
		if (lang != null) {
			key.append(';').append(lang);
		}
		appendCacheKey(key, request);
		return key.toString();
	}
	
	protected boolean contentTypeShouldBeZipped(String contentType) {
		return contentType != null && ( 
				contentType.equals("text/css") || 
				contentType.equals("text/javascript"));
	}

	public boolean gzipResponse(HttpServletRequest request) {
		try {
			Resource resource = lookupResource(getResourcePath(request));
			return contentTypeShouldBeZipped(getContentType(resource));
		}
		catch (IOException e) {
			return false;
		}
	}
	
	public ModelAndView handleRequest(HttpServletRequest request, 
            HttpServletResponse response) throws IOException {
        
    	String path = getResourcePath(request);
    	if (!checkForModifications) {
    		ServletUtils.setFarFutureExpiresHeader(response);
    	}
    	if (!serveResource(path, request, response)) {
    		response.sendError(HttpServletResponse.SC_NOT_FOUND);	
    	}
    	return null;
	}
	
	protected boolean serveResource(String path, HttpServletRequest request, 
			HttpServletResponse response) throws IOException {
		
    	Resource res = lookupResource(path);
		if (res != null) {
			String contentType = getContentType(res);
			response.setContentType(contentType);
			if (contentType.startsWith("text/")) {
				serveText(res, path, contentType, request, response.getWriter());
			}
			else {
				serveBinary(res, contentType, response.getOutputStream());
			}
			return true;
		}
		return false;
	}
	
	protected void serveText(Resource res, String path, String contentType,
			HttpServletRequest request, Writer out)	throws IOException {
		
		log.debug("Serving text resource: " + path);
		
		Reader in = getReader(res, path, contentType, request);
		FileCopyUtils.copy(in, out);
	}

	protected Reader getReader(Resource res, String path, String contentType,
			HttpServletRequest request) throws IOException {
		
		Reader in = new InputStreamReader(res.getInputStream(), "UTF-8");
		if (filters != null) {
			Iterator it = filters.iterator();
			while (it.hasNext()) {
				ResourceFilter filter = (ResourceFilter) it.next();
				if (filter.matches(path)) {
					log.debug("Filter " + filter + " matches.");
					in = filter.createFilterReader(in, request);
					break;
				}
				log.debug("Filter " + filter + " does not match.");
			}
		}
		return in;
	}
	
	protected void serveBinary(Resource res, String contentType, 
			OutputStream out) throws IOException {
		
		try {
			FileCopyUtils.copy(res.getInputStream(), out);
		}
		catch (IOException e) {
			if (!SocketException.class.isInstance(e.getCause())) {
				throw e;
			}
		}
	}

}
