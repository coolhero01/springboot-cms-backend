package com.oneclicktech.spring.util;

import java.math.BigDecimal;
import java.util.Map;

import org.apache.commons.lang3.math.NumberUtils;

public class NumberUtil extends NumberUtils {
	
	
	
	public static Long getLongValue(Map<String,Object> mapObj, String key) {
		Long valLong = 0L;
	    if (mapObj.get(key)!=null) {
	    	if (mapObj.get(key) instanceof Double) {
	    		valLong = ((Double)mapObj.get(key)).longValue();
			} else {
				valLong = (Long)mapObj.get(key);
			}
 		}
 		return valLong;
	}
	
	public static Double getDoubleValue(Map<String,Object> mapObj, String key) {
		Double valDoub= 0D;
	    if (mapObj.get(key)!=null) {
	    	if (mapObj.get(key) instanceof BigDecimal) {
	    		valDoub = ((BigDecimal)mapObj.get(key)).doubleValue();
			} else {
				valDoub = Double.parseDouble(String.valueOf(mapObj.get(key)));
			}
 		}
 		return valDoub;
	}
	
	
	public static Double roundTwoDec(Double val) {
		Double newVal = (Double) (val * 100);
		return (double) (Double.valueOf(Math.round(newVal)) / 100);
	}
	public static Integer getIntValue(Map<String,Object> mapObj, String key) {
		int intVal = 0;
	    if (mapObj.get(key)!=null) {
	    	if (mapObj.get(key) instanceof Double) {
	    		intVal = ((Double)mapObj.get(key)).intValue();
			} else {
				intVal = (Integer)mapObj.get(key);
			}
 		}
 		return intVal;
	}
}
