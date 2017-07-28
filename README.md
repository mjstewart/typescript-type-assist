# Typescript type assist

[![Youtube demo](https://github.com/mjstewart/typescript-type-assist/blob/master/resources/plugin-demo.png)](https://www.youtube.com/watch?v=wXIGN6lF8JI&feature=youtu.be "Plugin demo")

Typescript plugin for jetbrains IDE's including intellij and webstorm.

This typescript plugin enhances existing documentation and provides code generation for object properties.

### Features

* Documentation explaining the **shape** of non class types.
* Code generation for objects containing properties such as an interface or type alias.
* Assign a property value to a local variable including type information.
* Customisable settings for code style and documentation syntax colouring.

##### Unsupported

* Classes fall outside the scope of being basic type as they contain implementation details.
* The contents of a namespace.
* Display the documentation of inherited types within the sub type.
* Auto generate properties from inherited types within the sub type.

### Installation
1. settings > plugins > browse repositories > search for 'typescript type assist'
2. Alternatively download the `typescript-type-assist.jar` and go to settings > plugins > install plugin from disk. 

### FAQ

* Is there any reason why intentions don't show the custom plugin icon?

This appears to be a problem with all 2017 IDE versions.

* Sometimes when I click a documentation hyperlink nothing happens

This can occur if the IDE cannot resolve the type. This may happen if there is multiple type definitions for the 
same type.


