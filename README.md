## Eclipse Integration for Tapestry5

Yet another Design/Code switcher for Tapestry5 applications.

![Screenshot](https://f.cloud.github.com/assets/76579/1050715/4503627e-10be-11e3-8bf2-b9e6ccc2628b.png)

### Tapestry Context View

Go to *Window* -> *Show View* -> *Other...*, filter by "Tapestry" and select "Tapestry Context".

Tapestry Context View will appear (top right on this screenshot):

  - Provides context for opened Tapestry file
  - Includes code/template files of selected page/component, as well as list of properties files, @Import'ed and @Path @Inject'ed assets
  - Click on the file in a view to open this file
  - Simple validation implemented to highlight assets, JavaScript stacks & T5.4 modules that couldn't be resolved
    * supports default/`classpath:` or `context:` binding prefixes for assets
    * supports <a href="#tapestry-symbols">Tapestry symbols</a> expansion in asset path

![Screenshot](https://f.cloud.github.com/assets/76579/1105085/c106e906-1918-11e3-9525-68839dcc89b2.png)

### Quick Switch within Tapestry Context

Note: _This feature is only available in Eclipse 4._

Quick Switch provides the same navigation functionality as Tapestry Context view.
Use it when you work in maximized editor mode and/or your Tapestry Context view is minimized.

Default shortcut for this pop-up is `CMD+;`:

![Screenshot](https://f.cloud.github.com/assets/76579/1160288/8b93c87e-1fd6-11e3-8609-3ff77283dd38.png)

<div id="quick-switch-shortcut" /> 

**Tip:** If you installed Quick Switch feature, I recommend you to change key bindings vice versa:
`CMD+;` for Code/Design switch and `CMD+R` for the Quick Switch pop-up:


![Eclipse Preferences](https://f.cloud.github.com/assets/76579/1435240/a00a5b00-4138-11e3-9eed-603fa6359b59.png)

### Jump to Definition

CMD+Hover over component reference to jump to one of its definitions:

![Screenshot](https://f.cloud.github.com/assets/76579/1147100/4ee57f58-1e96-11e3-957d-1f875797620b.png)

### Completion Proposals

Tapestry5 completion proposals now available for all WTP Editors.

![parameter-proposal](https://f.cloud.github.com/assets/76579/1703919/dca5a70e-60be-11e3-8fe8-58f31f3b1a30.png)
![property-code](https://f.cloud.github.com/assets/76579/1703921/dca6dfe8-60be-11e3-8619-719382359ad6.png)
![property-proposal](https://f.cloud.github.com/assets/76579/1703920/dca65370-60be-11e3-87be-2c4c8137ec38.png)


### Tapestry Project Outline

Go to *Window* -> *Show View* -> *Other...*, filter by "Tapestry" and select "Tapestry Project Outline".

<img src="https://cloud.githubusercontent.com/assets/76579/5996402/084a58d2-aaba-11e4-9b06-a0eefe1ff862.png" width="600" alt="Tapestry Project Outline view">

This view displays structure of your Tapestry5 project:

  - Select your main Tapestry5 project (i.e. the project with `web.xml`) in the "Project Explorer" to see its outline
  - View all Tapestry5 modules available in classpath of the main Tapestry5 project
  - View [library mappings](http://tapestry.apache.org/component-libraries.html#ComponentLibraries-Step4%3AConfigurethevirtualfolder), [JavaScript stacks](https://tapestry.apache.org/javascript.html#JavaScript-JavaScriptStacks), [Tapestry services](http://tapestry.apache.org/defining-tapestry-ioc-services.html), [decorators](http://tapestry.apache.org/tapestry-ioc-decorators.html), [advisors](http://tapestry.apache.org/service-advisors.html), and [configuration contributions](http://tapestry.apache.org/tapestry-ioc-configuration.html) declared in each module (only available if source code attached to the module class)
  - View [symbols](http://tapestry.apache.org/symbols.html) declared in the project
  - Double click on Module name or JavaScript stack to open its source code
  - Double click on Library mapping, Service, Decorator, Advisor, Contribution, or Symbol opens their declaration
  - Selecting a item in Tapestry Project Outline view makes it appear in the Javadoc View and also highlights definition source range in a Java editor if the editor for this file is open & active
  - Content of this view will be updated automatically in background when you're adding new or updating existing modules, changing `web.xml`, or editing your pages/components files
  - More to come...

**Note:** Analysis of 3rd party modules will only work if you have sources attached to your JARs in Eclipse (which is the default if you import your project via `./gradlew eclipse` or `mvn eclipse:eclipse -DdownloadSources=true`).
If you don't have sources attached to one of your module classes you will see warning icon near it in the Tapestry Project Outline view.

**Note:** `CMD+Hover` feature won't be able to find components in the modules that don't have attached sources.

#### Support for JavaScript stack overrides

It is possible in Tapestry5 to override stack definitions, for example, this is what [Tapestry5 jQuery](https://github.com/got5/tapestry5-jquery/) does for core stacks. Overridden stacks will have special markers in Tapestry Project Outline view.

<img alt="stacks-overrides" src="https://cloud.githubusercontent.com/assets/76579/2624897/5fbc69c6-bd6b-11e3-923c-3c8609ba42af.png" width="640">

<img alt="stacks-overridden" src="https://cloud.githubusercontent.com/assets/76579/2624898/5fc00158-bd6b-11e3-9ee5-e4130ba5a23b.png" width="640">

<div id="tapestry-symbols" />

#### Support for Tapestry Symbols

All symbols declared in a project collected & presented in one place.

  - For every symbol you can find provider name (i.e. `ApplicationDefauls` or `FactoryDefaults`), tapestry module where this symbol declared, and even value of the symbol.
  - Markers for new, override, and overridden symbols.
  - Double-click on symbol to open source code of the declaration.

<img alt="tapestry-symbols" src="https://cloud.githubusercontent.com/assets/76579/6098386/931c3506-afed-11e4-87bb-0cf3f93b5407.png" width="640">

### Quickly Create Files for Tapestry5

#### Create new Tapestry5 pages, components and mixins quickly

Click to open standard *New Java Class* dialog with pre-filled source folder and package name fields.

![Tapestry5Toolbar](https://f.cloud.github.com/assets/76579/1435404/87d6ac0a-413d-11e3-89ec-00b4cd38a862.png)

#### Create new files in context

Create complement files for current Tapestry Context.

![ViewToolbar](https://f.cloud.github.com/assets/76579/1435416/d336ec14-413d-11e3-9815-470af70305c6.png)

Click to open standard *New File* dialog with pre-filled location and file-name fields:

![NewFile](https://f.cloud.github.com/assets/76579/1435414/d334a58a-413d-11e3-8a93-edb847abca58.png)

Tapestry templates and JavaScript files will be created with sample content.

![TemplateContent](https://f.cloud.github.com/assets/76579/1435417/d338eef6-413d-11e3-917b-c42806ce9d90.png)

Notice how cursor position is set in the new file -- it's right in the place you want to start extending it! :)

Creating JavaScript or CSS assets for page/component will add/modify `@Import` annotation by putting the reference to new asset.

![JavaScriptContent](https://f.cloud.github.com/assets/76579/1435415/d3352fe6-413d-11e3-9053-1a653c5840d8.png)

#### Edit templates

There are few very basic templates that come with the plugin.

Using Tapestry Context's *Edit Templates* menu it's now possible to edit those templates.

<img width="463" alt="edit-templates" src="https://cloud.githubusercontent.com/assets/76579/12533376/53f6dd3a-c23f-11e5-8c4f-ff753d4ecf8b.png">

Selecting a template from the list will open or create corresponding file under `src/main/eclipse-tapestry5`.

It's assumed that this folder will be committed to your repository and shared with your team.

`page.*` templates used when creating assets within a *page* Tapestry Context, `component.*` -- for *components*, etc.

Templates are not limited to just `*.js`, `*.css`, and `*.properties` -- you may add templates with your own file extensions manually, i.e. `page.coffee` or `page.less`.

It is possible to have more than one set of templates organized in sub-folders.

Templates in the root of `src/main/eclipse-tapestry5` will be used by default. Putting templates to sub-folder will override default templates for files created under that subfolder/package.

For example, to use different templates for all pages under `com.example.app.pages.admin` package, put your templates in `src/main/eclipse-tapestry5/admin` folder.

Two special snippets supported in templates: `$ContextName$` and `$Caret$`.

`$ContextName$` will be replaced with name of a Tapestry Context for which the file was created.

`$Caret$` used to position a cursor in a newly created file.

### Advanced Configuration

All plugin's configuration is stored in `src/main/eclipse-tapestry5/config.json` which is a JSON file with some additional formatting (see [GSON's lenient parsing mode](http://google.github.io/gson/apidocs/com/google/gson/stream/JsonReader.html#setLenient-boolean-)).

To create this file with default content click *Project Settings...* from Tapestry Context's menu.

The file has some inlined comments that should be self-explanatory, i.e. default `config.json` for Tapestry 5.3 can be found [here](https://raw.githubusercontent.com/anjlab/eclipse-tapestry5-plugin/master/com.anjlab.eclipse.tapestry5/src/com/anjlab/eclipse/tapestry5/templates/config-5.3.json).

#### File naming conventions

When creating new file within Tapestry Context it's possible to specify naming convention that should be used for a name of the file:

```
    # Naming conventions for new files. Supported values are:
    # UpperCamel, lowerCamel, lower_underscode, lower-hyphen
    
    fileNamingConventions: {
        
        *.js = UpperCamel
        
        , *.css = UpperCamel
        
        , *.* = UpperCamel
        
    }
```

For `*.tml` and `*.java` files naming convention is always `UpperCamel` and cannot be overridden.

Concrete convention is picked in the order from top to bottom using simple Glob pattern matching.

#### Other settings

Other settings can be used to help plugin to parse structure of your project, read inline comments for more details: [T5.3](https://raw.githubusercontent.com/anjlab/eclipse-tapestry5-plugin/master/com.anjlab.eclipse.tapestry5/src/com/anjlab/eclipse/tapestry5/templates/config-5.3.json) and [T5.4](https://raw.githubusercontent.com/anjlab/eclipse-tapestry5-plugin/master/com.anjlab.eclipse.tapestry5/src/com/anjlab/eclipse/tapestry5/templates/config.json).

This is still work in progress and is subject to change.


### Install

In Eclipse go to *Help* -> *Install New Software...* -> *Add...*

Use this update site URL:

    https://dl.bintray.com/anjlab/eclipse-tapestry5-plugin

Or drag &amp; drop this icon into a running Eclipse:

<a href="http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=1125032" title="Drag and drop into a running Eclipse workspace to install Eclipse Integration for Tapestry5">
  <img src="https://marketplace.eclipse.org/sites/all/modules/custom/marketplace/images/installbutton.png"/>
</a>

**Note:** Not sure if this is an Eclipse Marketplace bug, but for some reason when you install the plugin using drag &amp; drop you will only see one feature (Basic). If you install using update site then you should see all 3 features available.

### Thanks To

![YourKit](http://www.yourkit.com/images/yklogo.png)

YourKit supports open source projects with its full-featured Java Profiler.

YourKit, LLC is the creator of <a href="http://www.yourkit.com/java/profiler/index.jsp">YourKit Java Profiler</a>
and <a href="http://www.yourkit.com/.net/profiler/index.jsp">YourKit .NET Profiler</a>,
innovative and intelligent tools for profiling Java and .NET applications.
