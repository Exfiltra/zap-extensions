/*
 * Zed Attack Proxy (ZAP) and its related class files.
 *
 * ZAP is an HTTP/HTTPS proxy for assessing web application security.
 *
 * Copyright 2023 The ZAP Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package org.zaproxy.addon.postman;

import java.io.IOException;
import java.util.List;
import net.sf.json.JSONObject;
import org.zaproxy.zap.extension.api.ApiAction;
import org.zaproxy.zap.extension.api.ApiException;
import org.zaproxy.zap.extension.api.ApiImplementor;
import org.zaproxy.zap.extension.api.ApiResponse;
import org.zaproxy.zap.extension.api.ApiResponseElement;

public class PostmanApi extends ApiImplementor {
    private static final String PREFIX = "postman";
    private static final String ACTION_IMPORT_FILE = "importFile";
    private static final String ACTION_IMPORT_URL = "importUrl";
    private static final String PARAM_URL = "url";
    private static final String PARAM_FILE = "file";

    public PostmanApi() {
        this.addApiAction(new ApiAction(ACTION_IMPORT_FILE, List.of(PARAM_FILE), List.of()));
        this.addApiAction(new ApiAction(ACTION_IMPORT_URL, List.of(PARAM_URL), List.of()));
    }

    @Override
    public String getPrefix() {
        return PREFIX;
    }

    @Override
    public ApiResponse handleApiAction(String name, JSONObject params) throws ApiException {
        PostmanParser parser = new PostmanParser();

        switch (name) {
            case ACTION_IMPORT_FILE:
                try {
                    parser.importFromFile(params.getString(PARAM_FILE), "", false);
                } catch (IllegalArgumentException | IOException e) {
                    throw new ApiException(ApiException.Type.BAD_EXTERNAL_DATA);
                }
                break;
            case ACTION_IMPORT_URL:
                try {
                    parser.importFromUrl(params.getString(PARAM_URL), "", false);
                } catch (IllegalArgumentException | IOException e) {
                    throw new ApiException(ApiException.Type.BAD_EXTERNAL_DATA);
                }
                break;

            default:
                throw new ApiException(ApiException.Type.BAD_ACTION);
        }

        return ApiResponseElement.OK;
    }
}
