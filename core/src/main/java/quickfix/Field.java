/*******************************************************************************
 * Copyright (c) quickfixengine.org  All rights reserved. 
 * 
 * This file is part of the QuickFIX FIX Engine 
 * 
 * This file may be distributed under the terms of the quickfixengine.org 
 * license as defined by quickfixengine.org and appearing in the file 
 * LICENSE included in the packaging of this file. 
 * 
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING 
 * THE WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A 
 * PARTICULAR PURPOSE. 
 * 
 * See http://www.quickfixengine.org/LICENSE for licensing information. 
 * 
 * Contact ask@quickfixengine.org if any conditions of this licensing 
 * are not clear to you.
 ******************************************************************************/

package quickfix;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;

/**
 * Base class for FIX message fields. This class should be
 * abstract but that would break compatibility with the QF JNI
 * classes.
 * 
 * @author sbate
 *  
 */
public /*abstract*/ class Field implements Serializable{
    static final long serialVersionUID = 7098326013456432197L;
    private int tag;
    private Object object;
    private boolean isCalculated = false;
    private String data;
    
    public Field(int field, Object object) {
        this.tag = field;
        this.object = object;
    }

    /**
     * Gets the field's tag.
     * 
     * @return the tag
     */
    public int getTag() {
        return tag;
    }

    /**
     * Gets the field's tag. (QF/C++ compatibility)
     * 
     * @return the tag
     * @see quickfix.Field#getTag()
     */
    public int getField() {
        return getTag();
    }

    /**
     * Sets the field's value to the given object.
     * @param object
     */
    protected void setObject(Object object) {
        this.object = object;
        isCalculated = false;
    }

    /**
     * Get the field value
     * @return an object representing the field's value
     */
    public Object getObject() {
        return object;
    }

    
    /**
     * Return's the formatted field (tag=value<SOH>)
     * @return the formatted field
     */
    public String toString() {
        calculate();
        return data;
    }

    /*package*/ void toString(StringBuffer buffer) {
        buffer.append(tag).append('=').append(object.toString());
    }
    
    public boolean equals(Object object) {
        if (super.equals(object) == true)
            return true;
        if (!(object instanceof Field))
            return false;
        return tag == ((Field) object).getField()
                && getObject().equals(((Field) object).getObject());
    }

    public int hashCode() {
        return object.hashCode();
    }
    
    /*package*/ int getLength() {
        calculate();
        System.out.println(data+" "+byteCount(data));
        return byteCount(data)+1;
    }
    
    private int byteCount(String s) {
        return s.getBytes().length;
//        int length = 0;
//        for (int i = 0; i < s.length(); i++) {
//            int c = s.charAt(i);
//            length++;
//            if (c > 256) {
//                System.out.println("FOO "+c+" "+data.length());
//                length += (c & 0x0000FF00) != 0 ? 1 : 0;
//                length += (c & 0x00FF0000) != 0 ? 1 : 0;
//                length += (c & 0xFF000000) != 0 ? 1 : 0;
//            }
//        }
//        return length;
    }

    /*package*/int getTotal() {
        calculate();
        int sum = 0;
//        for (int i = 0; i < data.length(); i++) {
//            char c = data.charAt(i);
//            sum += c;
//            if (c > 256) {
//                sum += (c & 0x0000FF00) >> 8;
//                sum += (c & 0x00FF0000) >> 16;
//                sum += (c & 0xFF000000) >> 24;
//            }
//        }
//        return sum + 1;
        //todo: char or byte?
        try {
            byte[] byteData = data.getBytes("GBK");
            for (int i = 0; i < byteData.length; i++) {
                sum += ((int)byteData[i]) & 0xFF;
            }
            return sum + 1;
        } catch (UnsupportedEncodingException e) {
            // TODO Auto-generated catch block
           throw new RuntimeException(e);
          
        }
    }
    
    private void calculate() {
        if (isCalculated) {
            return;
        }
        
        StringBuffer buffer = new StringBuffer();
        toString(buffer);
        data = buffer.toString();
        
        isCalculated = true;
    }

    public void setTag(int tag) {
        this.tag = tag;
        isCalculated = false;
        calculate();
    }
}