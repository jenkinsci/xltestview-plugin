package com.xebialabs.xltest.fitnesse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class PageSummary {
    private final String pageName;
    private final Set<String> tags;
    private final Map<String, String> extraProperties;
    private final String eventType;

    public PageSummary(String pageName, Set<String> tags, String eventType) {
        this(pageName, tags, Collections.<String, String>emptyMap(), eventType);
    }

    public PageSummary(String pageName, Set<String> tags, Map<String, String> extraProperties, String eventType) {
        this.pageName = pageName;
        this.tags = tags;
        this.extraProperties = extraProperties;
        this.eventType = eventType;
    }

    public String getPageName() {
        return pageName;
    }

    public Set<String> getTags() {
        return tags;
    }

    public Map<String, String> getExtraProperties() {
        return extraProperties;
    }

    @Override
    public int hashCode() {
        return pageName.hashCode() ^ tags.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PageSummary)) return false;
        PageSummary other = (PageSummary) o;
        return other.pageName.equals(pageName) && other.tags.equals(tags);
    }

    @Override
    public String toString() {
        JSONObject obj = new JSONObject();
        try {
            // Do the extra properties first: we do not want to mess up the default properties.
            for (Map.Entry<String, String> kv : extraProperties.entrySet())
                obj.putOpt(kv.getKey(), kv.getValue());
            obj.put("type", eventType);
            obj.put("pageName", pageName);
            obj.put("tags", new JSONArray(tags));
        } catch (JSONException e) {
            throw new RuntimeException("Can not make JSON", e);
        }
        return obj.toString();
    }
}
