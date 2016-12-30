# Implementation of JSF view scope for Spring

The main idea is to put spring bean into JSF view scope,
that you can access from ```FacesContext.getCurrentInstance().getViewRoot().getViewMap()```

This module provides appropriate Spring annotations for related JSF scopes:

- @SpringScopeRequest - for request scope
- @SpringScopeSession - for session scope
- @SpringScopeView - for view scope

## Add to your project

You can add this artifact to your project using [JitPack](https://jitpack.io/#javaplugs/spring-jsf).  
All versions list, instructions for gradle, maven, ivy etc. can be found by link above.

To get latest commit use -SNAPSHOT instead version number.

## Configuration

Just add next line to your applicationContext.xml
```
<import resource="classpath:/com/github/javaplugs/jsf/jsfSpringScope.xml"/>
```


## Usage

In spring XML
```
<bean id="..." class="..." scope="view">
    <!-- whatever -->
</bean>
```
From java
```
import com.github.javaplugs.jsf.SpringScopeView;

@SpringScopeView
@Component
class WhateverBean {
    // Implementation
}
```