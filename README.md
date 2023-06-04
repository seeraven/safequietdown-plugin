# Safe Quiet Down Jenkins Plugin

## Introduction

This plugin lets you put Jenkins in quiet-down mode while still allowing any
downstream jobs of currently running jobs to complete. It is highly inspired
by the [lenient-shutdown-plugin] but treats pipelines like any other job and
does not provide functionality to put down individual slaves.


## Getting started

### Installation of the Plugin

To install the plugin, download the latest release from the
[Releases page] and install the plugin:

  - Open the `Manage Jenkins` page.
  - Open the `Manage Plugins` page.
  - Open the `Advanced` tab.
  - Under `Upload Plugin` click on `Choose file` and select the `safequietdown.hpi`
    file.
  - Click on the `Upload` button.


### Configuration

Open the `Manage Jenkins` page and then the `Configure System` page. Here, you'll
find the section `Safe Quietdown` where you have the following configuration items:

  - The `Quietdown Message` specifies the default message shown when the safe
    quietdown mode is activated.
  - The `Allow All Queued Items` flag indicates what to do with queued jobs when
    the safe quietdown mode is activated. If it is not selected, queued jobs are
    only allowed to proceed if they are a downstream job of a currently running
    job. On the other hand, if the option is checked, all queued jobs are allowed
    to proceed.
    Regardless of this flag, all currently running jobs and all their downstream
    jobs are allowed to finish.


### Activation of the Safe Quietdown Mode

To activate the safe quietdown mode, open the `Manage Jenkins` page. In the
`Uncategorized` section you'll find the link `Activate Safe Quietdown`. If
you click it, the safe quietdown mode is activated. To disable the
safe quietdown mode again, click the link `Deactivate Safe Quietdown`.


### Using the Jenkins CLI

You can also use the Jenkins CLI to activate or deactivate the safe quietdown
mode:

    wget http://<JenkinsURL>/jnlpJars/jenkins-cli.jar
    java -jar jenkins-cli.jar -s http://<JenkinsURL>/ -auth <user>:<password> safe-quiet-down -a -m "Triggerd from the CLI"
    java -jar jenkins-cli.jar -s http://<JenkinsURL>/ -auth <user>:<password> cancel-safe-quiet-down

In addition, the Jenkins CLI command `finished-safe-quiet-down` allows you
to check whether all permitted jobs are finished:

    java -jar jenkins-cli.jar -s http://<JenkinsURL>/ -auth <user>:<password> finished-safe-quiet-down && echo "You can shutdown now!"

However, if you want to use this command to savely shutdown Jenkins from a
script, you should probably ensure that about 3 attempts in a row give the
same result. For an example script, see [examples/safeJenkinsShutdown.sh].


## Contributing

To contribute to this plugin, fork this repository and create a new branch
containing your changes. Then open a pull request to integrate your changes
into the main branch.


## Build the Plugin

To build the plugin from source, you can use the following commands:

    $ make build

The final plugin is then found at `safequietdown-plugin/target/safequietdown.hpi`.

In addition to building the plugin, the Makefile provides the following additional
commands:

  - `make help` prints the usage information of the Makefile.
  - `make build` builds the plugin using the maven docker container and executing
    the command `mvn clean package`.
  - `make clean` cleans the workspace by deleting the `target` directory and
    temporary files.
  - `make site` generates the reports using the maven docker container and
    executing the command `mvn site`.
  - `make test` runs the unit tests. To select a single test class or test function,
    set the `TEST` variable:

        TEST=com.clemensrabe.jenkins.plugins.safequietdown.SafeQuietdownConfigurationTest#testAllowAllQueuedItemsSetting make test


## Publish a new Release

To publish a new release, perform the following steps:

    # Switch to a new branch named after the upcoming version
    $ git branch releases/v<major>.<minor>
    $ git checkout releases/v<major>.<minor>

    # Perform the version switch
    $ make release-prepare

    # Delete the temporary files
    rm -rf \? pom.xml.releaseBackup

    # Push the commits and tags to the repository
    git push --set-upstream origin releases/v<major>.<minor>

Then go ahead and create a merge request. Once it is merged to master, open
the [Releases page] and _Draft a new release_. Here, select the version tag,
enter the description and attach the file `target/safequietdown.hpi`.


## LICENSE

Licensed under MIT, see the [LICENSE] file.


[lenient-shutdown-plugin]: https://github.com/jenkinsci/lenient-shutdown-plugin
[Releases page]: https://github.com/seeraven/safequietdown-plugin/releases
[examples/safeJenkinsShutdown.sh]: examples/safeJenkinsShutdown.sh
[LICENSE]: LICENSE.md