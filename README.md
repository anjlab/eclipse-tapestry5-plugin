## Eclipse Integration for Tapestry5

Yet another Design/Code switcher for Tapestry5 applications.

![Screenshot](https://f.cloud.github.com/assets/76579/1050715/4503627e-10be-11e3-8bf2-b9e6ccc2628b.png)

### Tapestry Context View

Go to *Window* -> *Show View* -> *Other...*, filter by "Tapestry" and select "Tapestry Context".

Tapestry Context View will appear (top right on this screenshot):

  - Provides context for opened Tapestry file
  - Includes code/template files of selected page/component, as well as list of properties files, @Import'ed and @Path @Inject'ed assets
  - Click on the file in a view to open this file
  - Simple validation implemented to highlight assets & JavaScript stacks that couldn't be resolved (supports default/`classpath:` or `context:` binding prefixes for assets)

![Screenshot](https://f.cloud.github.com/assets/76579/1105085/c106e906-1918-11e3-9525-68839dcc89b2.png)

### Quick Switch within Tapestry Context

Note: _This feature is only available in Eclipse 4._

Quick Switch provides the same navigation functionality as Tapestry Context view.
Use it when you work in maximized editor mode and/or your Tapestry Context view is minimized.

Default shortcut for this pop-up is `CMD+;`:

![Screenshot](https://f.cloud.github.com/assets/76579/1160288/8b93c87e-1fd6-11e3-8609-3ff77283dd38.png)

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

![TapestryProjectOutline](https://f.cloud.github.com/assets/76579/1591215/cb6da226-529e-11e3-8db7-e482f5aedb11.png)

This view displays structure of your Tapestry5 project:

  - Select your main Tapestry5 project (i.e. the project with `web.xml`) in the "Project Explorer" to see its outline
  - View all Tapestry5 modules available in classpath of the main Tapestry5 project
  - View [library mappings](http://tapestry.apache.org/component-libraries.html#ComponentLibraries-Step4%3AConfigurethevirtualfolder), [JavaScript stacks](https://tapestry.apache.org/javascript.html#JavaScript-JavaScriptStacks), and [services](http://tapestry.apache.org/defining-tapestry-ioc-services.html) declared in each module (only available if source code attached to the module class)
  - Double click on Module name or JavaScript stack to open its source code
  - Double click on Library mapping or Service to open its declaration
  - Content of this view will be updated automatically in background when you're adding new or updating existing modules, changing `web.xml`, or editing your pages/components files
  - More to come...

**Note:** Analysis of 3rd party modules will only work if you have sources attached to your JARs in Eclipse (which is the default if you import your project via `./gradlew eclipse` or `mvn eclipse:eclipse -DdownloadSources=true`).
If you don't have sources attached to one of your module classes you will see warning icon near it in the Tapestry Project Outline view.

**Note:** `CMD+Hover` feature won't be able to find components in the modules that don't have attached sources.

#### Support for JavaScript stack overrides

It is possible in Tapestry5 to override stack definitions, for example, this is what [Tapestry5 jQuery](https://github.com/got5/tapestry5-jquery/) does for core stacks. Overridden stacks will have special markers in Tapestry Project Outline view.

<img alt="stacks-overrides" src="https://cloud.githubusercontent.com/assets/76579/2624897/5fbc69c6-bd6b-11e3-923c-3c8609ba42af.png" width="640">

<img alt="stacks-overridden" src="https://cloud.githubusercontent.com/assets/76579/2624898/5fc00158-bd6b-11e3-9ee5-e4130ba5a23b.png" width="640">

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

### Install

In Eclipse go to *Help* -> *Install New Software...* -> *Add...*

Use this update site URL:

    https://dl.bintray.com/anjlab/eclipse-tapestry5-plugin

Or drag &amp; drop this icon into a running Eclipse:

<a href="http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=1125032" title="Drag and drop into a running Eclipse workspace to install Eclipse Integration for Tapestry5">
  <img src="https://marketplace.eclipse.org/sites/all/modules/custom/marketplace/images/installbutton.png"/>
</a>

**Note:** Not sure if this is an Eclipse Marketplace bug, but for some reason when you install the plugin using drag &amp; drop you will only see one feature (Basic). If you install using update site then you should see all 3 features available.
