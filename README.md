## Eclipse Integration for Tapestry5

Yet another Design/Code switcher for Tapestry5 applications.

![Screenshot](https://f.cloud.github.com/assets/76579/1050715/4503627e-10be-11e3-8bf2-b9e6ccc2628b.png)

### Tapestry Context View

Go to *Window* -> *Show View* -> *Other...*, filter by "Tapestry" and select "Tapestry Context".

Tapestry Context View will appear (top right on this screenshot):

  - Provides context for opened Tapestry file
  - Includes code/template files of selected page/component, as well as list of properties files and @Import'ed assets
  - Click on the file in a view to open this file
  - Simple validation implemented to highlight assets that couldn't be resolved (supports default or 'context:' binding prefixes)

![Screenshot](https://f.cloud.github.com/assets/76579/1105085/c106e906-1918-11e3-9525-68839dcc89b2.png)

### Quick Switch within Tapestry Context

Note: _This feature is only available in Eclipse 4._

Quick Switch provides the same navigation functionality as Tapestry Context view.
Use it when you work in maximized editor mode and/or your Tapestry Context view is minimized.

Default shortcut for this pop-up is `CMD+;`:

![Screenshot](https://f.cloud.github.com/assets/76579/1160288/8b93c87e-1fd6-11e3-8609-3ff77283dd38.png)

### Jump to Definition

CMD+Hover over component reference to jump to one of its definitions:

![Screenshot](https://f.cloud.github.com/assets/76579/1147100/4ee57f58-1e96-11e3-957d-1f875797620b.png)

### Install

In Eclipse go to *Help* -> *Install New Software...* -> *Add...*

Use this update site URL:

    https://dl.bintray.com/anjlab/eclipse-tapestry5-plugin

Or drag &amp; drop this icon into a running Eclipse:

<a href="http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=1125032" title="Drag and drop into a running Eclipse workspace to install Eclipse Integration for Tapestry5">
  <img src="https://marketplace.eclipse.org/sites/all/modules/custom/marketplace/images/installbutton.png"/>
</a>
