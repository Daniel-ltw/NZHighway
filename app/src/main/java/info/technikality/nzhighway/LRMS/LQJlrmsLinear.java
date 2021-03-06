package info.technikality.nzhighway.LRMS;

//----------------------------------------------------
//
// Generated by www.easywsdl.com
// Version: 4.0.1.0
//
// Created by Quasar Development at 30-08-2014
//
//---------------------------------------------------


import java.util.Hashtable;
import org.ksoap2.serialization.*;

public class LQJlrmsLinear extends AttributeContainer implements KvmSerializable
{
    
    public LQJlrmsPoint end;
    
    public LQJlrmsPoint start;
    
    public String wktGeometry;

    public LQJlrmsLinear ()
    {
    }

    public LQJlrmsLinear (java.lang.Object paramObj,LQJExtendedSoapSerializationEnvelope envelope)
    {
	    
	    if (paramObj == null)
            return;
        AttributeContainer inObj=(AttributeContainer)paramObj;


        SoapObject soapObject=(SoapObject)inObj;  
        if (soapObject.hasProperty("end"))
        {	
	        java.lang.Object j = soapObject.getProperty("end");
	        this.end = (LQJlrmsPoint)envelope.get(j,LQJlrmsPoint.class);
        }
        if (soapObject.hasProperty("start"))
        {	
	        java.lang.Object j = soapObject.getProperty("start");
	        this.start = (LQJlrmsPoint)envelope.get(j,LQJlrmsPoint.class);
        }
        if (soapObject.hasProperty("wktGeometry"))
        {	
	        java.lang.Object obj = soapObject.getProperty("wktGeometry");
            if (obj != null && obj.getClass().equals(SoapPrimitive.class))
            {
                SoapPrimitive j =(SoapPrimitive) obj;
                if(j.toString()!=null)
                {
                    this.wktGeometry = j.toString();
                }
	        }
	        else if (obj!= null && obj instanceof String){
                this.wktGeometry = (String)obj;
            }    
        }


    }

    @Override
    public java.lang.Object getProperty(int propertyIndex) {
        //!!!!! If you have a compilation error here then you are using old version of ksoap2 library. Please upgrade to the latest version.
        //!!!!! You can find a correct version in Lib folder from generated zip file!!!!!
        if(propertyIndex==0)
        {
            return end!=null?end:SoapPrimitive.NullSkip;
        }
        if(propertyIndex==1)
        {
            return start!=null?start:SoapPrimitive.NullSkip;
        }
        if(propertyIndex==2)
        {
            return wktGeometry!=null?wktGeometry:SoapPrimitive.NullSkip;
        }
        return null;
    }


    @Override
    public int getPropertyCount() {
        return 3;
    }

    @Override
    public void getPropertyInfo(int propertyIndex, @SuppressWarnings("rawtypes") Hashtable arg1, PropertyInfo info)
    {
        if(propertyIndex==0)
        {
            info.type = LQJlrmsPoint.class;
            info.name = "end";
            info.namespace= "";
        }
        if(propertyIndex==1)
        {
            info.type = LQJlrmsPoint.class;
            info.name = "start";
            info.namespace= "";
        }
        if(propertyIndex==2)
        {
            info.type = PropertyInfo.STRING_CLASS;
            info.name = "wktGeometry";
            info.namespace= "";
        }
    }
    
    @Override
    public void setProperty(int arg0, java.lang.Object arg1)
    {
    }

}
