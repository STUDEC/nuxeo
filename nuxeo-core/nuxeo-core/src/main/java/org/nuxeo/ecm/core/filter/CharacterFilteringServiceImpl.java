/*
 * (C) Copyright 2017 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Funsho David
 *
 */

package org.nuxeo.ecm.core.filter;

import com.google.common.base.CharMatcher;
import org.apache.commons.lang.StringEscapeUtils;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.model.Property;
import org.nuxeo.ecm.core.api.model.impl.ArrayProperty;
import org.nuxeo.ecm.core.api.model.impl.ComplexProperty;
import org.nuxeo.ecm.core.api.model.impl.ListProperty;
import org.nuxeo.ecm.core.api.model.impl.primitives.StringProperty;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

import java.util.Collection;
import java.util.List;

/**
 * @since 9.1
 */
public class CharacterFilteringServiceImpl extends DefaultComponent implements CharacterFilteringService {

    public static final String FILTERING_XP = "filtering";

    protected CharacterFilteringServiceDescriptor desc;

    @Override
    public void registerContribution(Object contrib, String point, ComponentInstance contributor) {
        if (FILTERING_XP.equals(point)) {
            desc = (CharacterFilteringServiceDescriptor) contrib;
        } else {
            throw new RuntimeException("Unknown extension point: " + point);
        }
    }

    @Override
    public boolean isFilteringEnabled() {
        return desc.isEnabled();
    }

    @Override
    public String filter(CharMatcher charsToRemove, String value) {
        return charsToRemove.removeFrom(value);
    }

    @Override
    public void filterChars(DocumentModel docModel) {
        CharMatcher charsToPreserve = CharMatcher.anyOf("\r\n\t");
        CharMatcher allButPreserved = charsToPreserve.negate();
        CharMatcher controlCharactersToRemove = CharMatcher.JAVA_ISO_CONTROL.and(allButPreserved);
        controlCharactersToRemove = controlCharactersToRemove.or(
                CharMatcher.INVISIBLE.and(CharMatcher.WHITESPACE.negate()));

        List<String> additionalChars = desc.getUnallowedChars();
        String otherCharsToRemove = "";
        if (additionalChars != null && !additionalChars.isEmpty()) {
            for (String c : additionalChars) {
                otherCharsToRemove += StringEscapeUtils.unescapeJava(c);
            }
            controlCharactersToRemove = controlCharactersToRemove.or(CharMatcher.anyOf(otherCharsToRemove));
        }

        String[] schemas = docModel.getSchemas();
        for (String schema : schemas) {
            Collection<Property> properties = docModel.getPropertyObjects(schema);
            for (Property prop : properties) {
                filterProperty(controlCharactersToRemove, prop, docModel);
            }
        }
    }

    private void filterProperty(CharMatcher controlChars, Property prop, DocumentModel docModel) {
        if (prop instanceof StringProperty && prop.getValue() != null) {
            String filteredProp = filter(controlChars, (String) prop.getValue());
            docModel.setPropertyValue(prop.getPath(), filteredProp);
        } else if (prop instanceof ArrayProperty && prop.getValue() != null && prop.getValue() instanceof String[]) {
            String[] arrayProp = (String[]) prop.getValue();
            for (int i = 0; i < arrayProp.length; i++) {
                arrayProp[i] = arrayProp[i] != null ? filter(controlChars, arrayProp[i]) : null;
            }
            docModel.setPropertyValue(prop.getPath(), arrayProp);
        } else if (prop instanceof ComplexProperty) {
            ComplexProperty complexProp = (ComplexProperty) prop;
            for (Property subProp : complexProp.getChildren()) {
                filterProperty(controlChars, subProp, docModel);
            }
        } else if (prop instanceof ListProperty) {
            ListProperty listProp = (ListProperty) prop;
            for (Property p : listProp) {
                filterProperty(controlChars, p, docModel);
            }
        }
    }
}
