/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.riotfamily.core.screen.list;

import java.io.Serializable;

import javax.servlet.http.HttpServletRequest;

import org.riotfamily.common.web.support.ServletUtils;
import org.riotfamily.core.screen.ScreenLink;

public class ChooserSettings implements Serializable {

	private String targetScreenId;
	
	private String startScreenId;
	
	
	public ChooserSettings(String targetScreenId, String startScreenId) {
		this.targetScreenId = targetScreenId;
		this.startScreenId = startScreenId;
	}

	public ChooserSettings(HttpServletRequest request) {
		this(request.getParameter("choose"), request.getParameter("start"));
	}

	public ScreenLink appendTo(ScreenLink link) {
		StringBuffer url = new StringBuffer(link.getUrl());
		appendTo(url);
		link.setUrl(url.toString());
		return link;
	}
	
	public String appendTo(String url) {
		StringBuffer sb = new StringBuffer(url);
		appendTo(sb);
		return sb.toString();
	}
	
	private void appendTo(StringBuffer sb) {
		if (targetScreenId != null) {
			ServletUtils.appendParameter(sb, "choose", targetScreenId);
			ServletUtils.appendParameter(sb, "start", startScreenId);
		}
	}
	
	public String getTargetScreenId() {
		return targetScreenId;
	}

	public String getStartScreenId() {
		return startScreenId;
	}

}
