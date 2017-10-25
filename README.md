# POJO Translator/Mapper Generator

- Generates translators or deep cloners for POJOs, especially for JAXB generated classes.
- Generates calls to all setters or to all getters for a selected variable. 
       

Work in progress, contributors welcomed.

https://github.com/krasa/translator-generator-intellij-plugin-test
## Usage:
Alt + Insert on top of a variable, or inside of a method which takes one parameter and returns one, or takes two and returns none.

## Features:

Generating translators:

![](http://i.imgur.com/DOMa9FN.gif)
https://github.com/krasa/translator-generator-intellij-plugin-test/blob/master/src/main/java/Translator.java

Generating getter/setter calls: 

![]( http://i.imgur.com/mDnEmPj.gif) 
