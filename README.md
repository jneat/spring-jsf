# Implementation of JSF view scope for Spring

The main idea is to put spring bean into JSF view scope,
that you can access from ```FacesContext.getCurrentInstance().getViewRoot().getViewMap()```

This module provides appropriate Spring annotations for related JSF scopes:

- @SpringScopeRequest - for request scope
- @SpringScopeSession - for session scope
- @SpringScopeView - for view scope

## Adding to your project

For now this package is available only via https://jitpack.io/

### Gradle dependencies
```
repositories {
  maven {
    url "https://jitpack.io"
  }
}

dependencies {
  compile 'com.github.javaplugs:spring-jsf:0.1.1'
}
```

### Maven dependencies
```
<repository>
  <id>jitpack.io</id>
  <url>https://jitpack.io</url>
</repository>

<dependency>
  <groupId>com.github.javaplugs</groupId>
  <artifactId>spring-jsf</artifactId>
  <version>0.1.1</version>
</dependency>
```

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