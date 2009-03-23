package org.apache.maven.project.processor;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.codehaus.plexus.util.xml.Xpp3Dom;

public class PluginProcessor
    extends BaseProcessor
{
    public void process( Object parent, Object child, Object target, boolean isChildMostSpecialized )
    {
        super.process( parent, child, target, isChildMostSpecialized );
        List<Plugin> t = (List<Plugin>) target;

        if ( parent == null && child == null )
        {
            return;
        }
        else if ( parent == null && child != null )
        {
            
            boolean isAdd = true;
            Plugin targetPlugin = find((Plugin) child, t);
            if(targetPlugin == null) 
            {
                targetPlugin = new Plugin();
            }
            else
            {
                isAdd = false;
            }
            
            copy( (Plugin) child, targetPlugin, isChildMostSpecialized );
            if(isAdd) t.add( targetPlugin );
        }
        else if ( parent != null && child == null )
        {            
            boolean isAdd = true;
            Plugin targetPlugin = find((Plugin) parent, t);
            if(targetPlugin == null) 
            {
                targetPlugin = new Plugin();
            }
            else
            {
                isAdd = false;
            }
            
            copy( (Plugin) parent, targetPlugin, false );
            if(isAdd) t.add( targetPlugin );
        }
        else
        // JOIN
        {          
            if( match( (Plugin) parent, (Plugin) child) )
            {
                boolean isAdd = true;
                Plugin targetPlugin = find((Plugin) parent, t);
                if(targetPlugin == null) 
                {
                    targetPlugin = new Plugin();
                }
                else
                {
                    isAdd = false;
                }                 
                copy( (Plugin) parent, targetPlugin, false );
                copy( (Plugin) child, targetPlugin, isChildMostSpecialized );
                if(isAdd) t.add( targetPlugin ); 
            } 
            else
            {
                Plugin targetPlugin = new Plugin();
                copy( (Plugin) parent, targetPlugin, false );
                copy( (Plugin) child, targetPlugin, isChildMostSpecialized );
                t.add( targetPlugin );    
            }  
        }       
    }
    
    private static Plugin find(Plugin p1, List<Plugin> plugins)
    {
        for(Plugin t : plugins)
        {
            if(match(p1, t)){
                return t;
            }
        }
        
        return null;
    }
    
    private static boolean match( Plugin d1, Plugin d2 )
    {
        return getId( d1 ).equals( getId( d2 ));
    }   
    
    private static String getId( Plugin d )
    {
        StringBuilder sb = new StringBuilder();
        sb.append( d.getGroupId() ).append( ":" ).append( d.getArtifactId() ).append( ":" );
        return sb.toString();
    }       
    
    private static void copy(Plugin source, Plugin target, boolean isChild)
    {
        if(!isChild && source.getInherited() != null && !source.getInherited().equalsIgnoreCase( "true" ))
        {
            return;
        }
        
        if(source.getArtifactId() != null)
        {
            target.setArtifactId( source.getArtifactId() );   
        }
        
        target.setGroupId( source.getGroupId() );    
        
        if(source.getInherited() != null)
        {
            target.setInherited( source.getInherited() );    
        }
        
        if(source.getVersion() != null)
        {
            target.setVersion( source.getVersion() );    
        }
        
        
        for( PluginExecution pe : source.getExecutions())
        {
            PluginExecution idMatch = contains(pe, target.getExecutions());
            if(idMatch != null)//Join
            {
               copyPluginExecution(pe, idMatch, isChild);    
            }
            else 
            {
                PluginExecution targetPe = new PluginExecution();
                copyPluginExecution(pe, targetPe, isChild); 
                target.addExecution( targetPe );
            }
            
        }
     
        DependenciesProcessor proc = new DependenciesProcessor();
        if(target.getDependencies().isEmpty())
        {
            
            proc.process( new ArrayList<Dependency>(), new ArrayList<Dependency>(source.getDependencies()), target.getDependencies(), false );            
        }
        else
        {
            proc.process( new ArrayList<Dependency>(source.getDependencies()), new ArrayList<Dependency>(), target.getDependencies(), false );            
        }

        if(source.getConfiguration() != null)
        {
            //TODO: Not copying
            if(target.getConfiguration() != null)
            {
                target.setConfiguration( Xpp3Dom.mergeXpp3Dom( (Xpp3Dom) source.getConfiguration(), (Xpp3Dom) target.getConfiguration() ));     
            }
            else
            {
                target.setConfiguration( source.getConfiguration() );
            }
                
        }
       
       // p2.setConfiguration( configuration ) merge nodes
        //Goals
        target.setExtensions(source.isExtensions()); 
        
    }
    
    private static PluginExecution contains(PluginExecution pe, List<PluginExecution> executions)
    {
        String executionId = (pe.getId() != null) ? pe.getId() : "";
        for(PluginExecution e : executions)
        {
            String id = (e.getId() != null) ? e.getId() : "";
            if(executionId.equals( id ))
            {
                return  e;
            }
        }
        return null;
    }
    
    private static void copyPluginExecution(PluginExecution source, PluginExecution target, boolean isChild)
    {
        if(!isChild && source.getInherited() != null && !source.getInherited().equalsIgnoreCase( "true" ))
        {
            return;
        }            
        target.setId( source.getId() );
        
        if(isChild && source.getInherited() != null)
        {
            target.setInherited( source.getInherited() );
        }
           
        if(source.getPhase() != null)
        {
            target.setPhase( source.getPhase() );
        }
        
        List<String> targetGoals = new ArrayList<String>(target.getGoals());
        List<String> setGoals = new ArrayList<String>();
        for(String goal : source.getGoals())
        {
            if(targetGoals.contains( goal ))
            {
                targetGoals.remove( goal );   
            }          
        }   
        setGoals.addAll( source.getGoals() );
        setGoals.addAll( targetGoals );
        target.setGoals( setGoals );
              
        if(source.getConfiguration() != null)
        {
            if(target.getConfiguration() != null)
            {
                target.setConfiguration( Xpp3Dom.mergeXpp3Dom( (Xpp3Dom) source.getConfiguration(), (Xpp3Dom) target.getConfiguration() ));     
            }
            else
            {
                target.setConfiguration( source.getConfiguration() );
            }            
        }
      
    }
}