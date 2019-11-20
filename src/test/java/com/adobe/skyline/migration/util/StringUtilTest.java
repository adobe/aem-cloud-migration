/*
 Copyright 2019 Adobe. All rights reserved.
 This file is licensed to you under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License. You may obtain a copy
 of the License at http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software distributed under
 the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR REPRESENTATIONS
 OF ANY KIND, either express or implied. See the License for the specific language
 governing permissions and limitations under the License.
 */

package com.adobe.skyline.migration.util;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class StringUtilTest {

   @Test
   public void testGetListOneItem() {
       List<String> returnList = StringUtil.getListFromString("single-item");
       assertEquals(1, returnList.size());
       assertEquals("single-item", returnList.get(0));
   }

    @Test
    public void testGetListBrackets() {
        List<String> returnList = StringUtil.getListFromString("[one, two]");
        assertEquals(2, returnList.size());
        assertTrue(returnList.contains("one"));
        assertTrue(returnList.contains("two"));
    }

    @Test
    public void testGetListNoBrackets() {
        List<String> returnList = StringUtil.getListFromString("one,two");
        assertEquals(2, returnList.size());
        assertTrue(returnList.contains("one"));
        assertTrue(returnList.contains("two"));
    }

}