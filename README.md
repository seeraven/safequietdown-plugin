# Safe Quiet Down Jenkins Plugin

## Introduction

This plugin lets you put Jenkins in quiet-down mode while still allowing any
downstream jobs of currently running jobs to complete. It is highly inspired
by the [lenient-shutdown-plugin](https://github.com/jenkinsci/lenient-shutdown-plugin)
but treats pipelines like any other job and does not provide functionality to
put down individual slaves.

## Getting started

TODO Tell users how to configure your plugin here, include screenshots, pipeline examples and 
configuration-as-code examples.

## Contributing

To contribute to this plugin, fork this repository and create a new branch
containing your changes. Then open a pull request to integrate your changes
into the main branch.

## Build the Plugin

To build the plugin from source, you can use the following commands:

    $ make build

The final plugin is then found at `safequietdown-plugin/target/safequietdown.hpi`.

## LICENSE

Licensed under MIT, see [LICENSE](LICENSE.md)

