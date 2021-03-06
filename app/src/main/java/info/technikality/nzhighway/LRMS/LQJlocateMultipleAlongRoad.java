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
import java.util.ArrayList;
import org.ksoap2.serialization.PropertyInfo;

public class LQJlocateMultipleAlongRoad extends AttributeContainer implements KvmSerializable
{
    
    public Integer roadId=0;
    
    public ArrayList< Double>displacements =new ArrayList<Double>();
    
    public Boolean displacementsAreFromRS=false;
    
    public Integer srid=0;

    public LQJlocateMultipleAlongRoad ()
    {
    }

    public LQJlocateMultipleAlongRoad (java.lang.Object paramObj,LQJExtendedSoapSerializationEnvelope envelope)
    {
	    
	    if (paramObj == null)
            return;
        AttributeContainer inObj=(AttributeContainer)paramObj;


        SoapObject soapObject=(SoapObject)inObj;  
        if (soapObject.hasProperty("roadId"))
        {	
	        java.lang.Object obj = soapObject.getProperty("roadId");
            if (obj != null && obj.getClass().equals(SoapPrimitive.class))
            {
                SoapPrimitive j =(SoapPrimitive) obj;
                if(j.toString()!=null)
                {
                    this.roadId = Integer.parseInt(j.toString());
                }
	        }
	        else if (obj!= null && obj instanceof Integer){
                this.roadId = (Integer)obj;
            }    
        }
        if (soapObject.hasProperty("displacements"))
        {	
	        int size = soapObject.getPropertyCount();
	        this.displacements = new ArrayList<Double>();
	        for (int i0=0;i0< size;i0++)
	        {
	            PropertyInfo info=new PropertyInfo();
	            soapObject.getPropertyInfo(i0, info);
                java.lang.Object obj = info.getValue();
	            if (obj!=null && info.name.equals("displacements"))
	            {
                    java.lang.Object j =info.getValue();
	                Double j1= new Double(j.toString());
	                this.displacements.add(j1);
	            }
	        }
        }
        if (soapObject.hasProperty("displacementsAreFromRS"))
        {	
	        java.lang.Object obj = soapObject.getProperty("displacementsAreFromRS");
            if (obj != null && obj.getClass().equals(SoapPrimitive.class))
            {
                SoapPrimitive j =(SoapPrimitive) obj;
                if(j.toString()!=null)
                {
                    this.displacementsAreFromRS = new Boolean(j.toString());
                }
	        }
	        else if (obj!= null && obj instanceof Boolean){
                this.displacementsAreFromRS = (Boolean)obj;
            }    
        }
        if (soapObject.hasProperty("srid"))
        {	
	        java.lang.Object obj = soapObject.getProperty("srid");
            if (obj != null && obj.getClass().equals(SoapPrimitive.class))
            {
                SoapPrimitive j =(SoapPrimitive) obj;
                if(j.toString()!=null)
                {
                    this.srid = Integer.parseInt(j.toString());
                }
	        }
	        else if (obj!= null && obj instanceof Integer){
                this.srid = (Integer)obj;
            }    
        }


    }

    @Override
    public java.lang.Object getProperty(int propertyIndex) {
        //!!!!! If you have a compilation error here then you are using old version of ksoap2 library. Please upgrade to the latest version.
        //!!!!! You can find a correct version in Lib folder from generated zip file!!!!!
        if(propertyIndex==0)
        {
            return roadId;
        }
        if(propertyIndex==1)
        {
            return displacementsAreFromRS;
        }
        if(propertyIndex==2)
        {
            return srid;
        }
        if(propertyIndex>=+3 && propertyIndex< + 3+this.displacements.size())
        {
            return displacements.get(propertyIndex-(+3));
        }
        return null;
    }


    @Override
    public int getPropertyCount() {
        return 3+displacements.size();
    }

    @Override
    public void getPropertyInfo(int propertyIndex, @SuppressWarnings("rawtypes") Hashtable arg1, PropertyInfo info)
    {
        if(propertyIndex==0)
        {
            info.type = PropertyInfo.INTEGER_CLASS;
            info.name = "roadId";
            info.namespace= "";
        }
        if(propertyIndex==1)
        {
            info.type = PropertyInfo.BOOLEAN_CLASS;
            info.name = "displacementsAreFromRS";
            info.namespace= "";
        }
        if(propertyIndex==2)
        {
            info.type = PropertyInfo.INTEGER_CLASS;
            info.name = "srid";
            info.namespace= "";
        }
        if(propertyIndex>=+3 && propertyIndex <= +3+this.displacements.size())
        {
            info.type = Double.class;
            info.name = "displacements";
            info.namespace= "";
        }
    }
    
    @Override
    public void setProperty(int arg0, java.lang.Object arg1)
    {
    }

}
