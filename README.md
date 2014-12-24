# Implementation of JSF view scope for Spring

[![Release](https://jitpack.io/v/jneat/spring-jsf.svg)](https://jitpack.io/#jneat/spring-jsf)  

The main idea is to put spring bean into JSF view scope,
that you can access from ```FacesContext.getCurrentInstance().getViewRoot().getViewMap()```

This module provides appropriate Spring annotations for related JSF scopes:

- @SpringScopeRequest - for request scope
- @SpringScopeSession - for session scope
- @SpringScopeView - for view scope

## Add to your project

You can add this artifact to your project using [JitPack](https://jitpack.io/#jneat/spring-jsf).  
All versions list, instructions for gradle, maven, ivy etc. can be found by link above.

To get latest commit use -SNAPSHOT instead version number.

## Configuration

Just add next line to your applicationContext.xml
```
<import resource="classpath:/com/github/jneat/jsf/jsfSpringScope.xml"/>
```


## Usage

### XML configuration
```
<bean id="..." class="..." scope="view">
    <!-- whatever -->
</bean>
```

### Java configuration
```
import java.util.HashMap;
import java.util.Map;

import com.github.jneat.jsf.ViewScope;
import org.springframework.beans.factory.config.CustomScopeConfigurer;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyViewScope extends CustomScopeConfigurer {

    public InitViewScope() {
        log.info("Init ViewScope");
        Map<String, Object> map = new HashMap<>();
        map.put("view", new ViewScope());
        super.setScopes(map);
    }
}
```


### In your code
```
import com.github.jneat.jsf.SpringScopeView;

@SpringScopeView
@Component
class WhateverBean {
    // Implementation
}
```