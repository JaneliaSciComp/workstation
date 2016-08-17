/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package org.janelia.pretty_print.convertor;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

/**
 * Converts the input to a prettier format.
 * Borrowed from 
 *  http://stackoverflow.com/questions/4105795/pretty-print-json-in-java
 *
 * @author fosterl
 */
public class PrettyPrinter {
    public PrettyPrinter() {
        
    }

	/**
	 * Pretty prints a JSon string for indentation and vertical arrangement.
	 * 
	 * Example:  { "who": ["you","me","them"], "where":"here", "when":"now" }
	 * 
	 * @param uglyJSONString like above.
	 * @return easier-to-read version.
	 */
    public String convert(String uglyJSONString) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonParser jp = new JsonParser();
            JsonElement je = jp.parse(uglyJSONString);
            String prettyJsonString = gson.toJson(je);
            return prettyJsonString;
        } catch (Exception ex) {
            ex.printStackTrace();
            return uglyJSONString;
        }
    }
}
