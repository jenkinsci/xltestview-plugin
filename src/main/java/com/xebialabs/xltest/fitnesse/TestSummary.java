package com.xebialabs.xltest.fitnesse;

import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.json.JSONObject;

public class TestSummary {
    private final String pageName;
    private final Set<String> tags;
    private final long timestamp;
    private final long duration;
    private final int right;
    private final int wrong;
    private final int exceptions;
    private final String firstError;
    private final Map<String, String> pageProperties;

    public TestSummary(String pageName, Set<String> tags, long timestamp, long duration, int right, int wrong, int exceptions, String firstError, Map<String, String> pageProperties) {
        this.pageName = pageName;
        this.tags = tags;
        this.timestamp = timestamp;
        this.duration = duration;
        this.right = right;
        this.wrong = wrong;
        this.exceptions = exceptions;
        this.firstError = firstError;
        this.pageProperties = pageProperties;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (duration ^ (duration >>> 32));
		result = prime * result + exceptions;
		result = prime * result + ((pageName == null) ? 0 : pageName.hashCode());
		result = prime * result + ((pageProperties == null) ? 0 : pageProperties.hashCode());
		result = prime * result + right;
		result = prime * result + ((tags == null) ? 0 : tags.hashCode());
		result = prime * result + (int) (timestamp ^ (timestamp >>> 32));
		result = prime * result + wrong;
        result = prime * result + ((firstError == null) ? 0 : firstError.hashCode());
		return result;
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TestSummary other = (TestSummary) obj;
		if (duration != other.duration)
			return false;
		if (exceptions != other.exceptions)
			return false;
		if (pageName == null) {
			if (other.pageName != null)
				return false;
		} else if (!pageName.equals(other.pageName))
			return false;
		if (pageProperties == null) {
			if (other.pageProperties != null)
				return false;
		} else if (!pageProperties.equals(other.pageProperties))
			return false;
		if (right != other.right)
			return false;
		if (tags == null) {
			if (other.tags != null)
				return false;
		} else if (!tags.equals(other.tags))
			return false;
		if (timestamp != other.timestamp)
			return false;
		if (wrong != other.wrong)
			return false;
        if (firstError == null) {
            if (other.firstError != null)
                return false;
        } else if (!firstError.equals(other.firstError))
            return false;
		return true;
	}



	@Override
    public String toString() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", "result");
            obj.put("pageName", pageName);
            obj.put("tags", tags);
            obj.put("timestamp", timestamp);
            obj.put("duration", duration);
            obj.put("right", right);
            obj.put("wrong", wrong);
            obj.put("exceptions", exceptions);
            JSONObject pagePropertiesAsJSON = new JSONObject();
            for (String pagePropertyName : pageProperties.keySet()) {
            	pagePropertiesAsJSON.put(pagePropertyName, pageProperties.get(pagePropertyName));
            }
            obj.put("properties", pagePropertiesAsJSON);
            if (firstError != null) {
                obj.put("firstError", firstError);
            }
        } catch (JSONException e) {
            throw new RuntimeException("Can not make JSON", e);
        }
        return obj.toString();
    }
}
