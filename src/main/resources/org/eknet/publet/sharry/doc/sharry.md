## Sharry - Sharing Files

This extension allows to share files with others in a simple way. Sharry aims
to be a good mix of secure and easy to use.

### How it works

#### Authenticated users -> others

Authenticated users can share files with others by uploading their files on a page
together with an optional password and a timespan. The files are then uploaded to
the server that will first put them all in a single zip archive and then encrypt
the archive with a symmetic cipher (AES) using either the given password or it
generates a random one. The next screen displays the new information: the download
link, the password and the date until the link is valid. It is possible to send
this information via email to other people.

The download is open to everyone. It offers to download the zip archive and expects
a password. If the password is wrong, the zip archive on the server can simply not
be decrypted and thus the user receives an empty file. If the password is correct,
the decryption is successful and the zip archive is sent to the client.

#### Others -> Authenticated users

It's also possible to allow anonymous users to send files to registered ones. For
this each registered user can maintain _alias pages_. An alias page just presents
a simple upload form and is open to everyone. The form does not allow to specify
a custom password or validation time span in contrast to the upload form for
authenticated users. The files uploaded through this page are stored on the server
and the user that belongs to the alias is notified.

The links to those pages can be created, deleted or disabled by the registered
users. They are meant to be shared with others. It is possible to define default
passwords and a validation time span for each alias. The links to the alias pages
are random and "hard to guess", but, to make any sense, they publicly accessible.
If spam starts to occur, the alias page can be deleted or disabled.

### Configuration


#### Mounting

The resources are mounted by default to the path `/sharry`. This can be changed by
adding a custom one to `publet.properties` configuration file:

    sharry.path=/another/path/to/sharry

#### Maximum Size

The maximum size allowed to store the archives can be configured using this property:

    sharry.maxFolderSize=500MiB

The size is given using a number followed by a unit. The unit may be one of: `bytes`,
`kib`, `mib` or `gib`. The case does not matter. You can change the property and
reload the config to set a new value without application restart.

#### Maximum Upload Size

One upload is restricted to a certain size. It is by default `100MiB` and can be
adjusted in `publet.properties`:

    sharry.maxUploadSize=200MiB

The size string must be in same format as described above. You can change the property
and reload the config to set a new value without application restart.

#### Delete Job

A job is scheduled on publet startup that will look through all archives and deletes
those that have expired. The job is by default scheduled to run every 24 hours. The
interval can be changed in `publet.properties`:

    sharry.deleteJobInterval=12

The value is the number of hours to wait until the next run of the job. If you change
and reload the config file, the job is re-scheduled.


### Usage

The page at `sharry/index.html` (while `sharry` is the default path) is the
entrypoint for auhtenticated users. The users must have the permission

    sharry:usage

to access the page and all non-public json scripts in this extension. You should also
grant the users the `resource` permission to access this page. This page contains three
widgets that are provided as jquery plugins:

* the upload form
* a widget to manage your archives ("manage" means: go to the download page, or delete the archive)
* a widget to manage your aliases

Those widgets can also be used in custom pages. You must add the asset group `publet-sharry.assets`
and use the jquery plugins:

    $(function() {
      $('#sharry').sharry(); // the upload form
      $('#sharryArchives').sharryArchives(); //manage archives
      $('#sharryAliases').sharryAlias(); //manage aliases
    });

The plugin `$('#sharry').sharry();` creates the upload form for authenticated users. You can
specify an alias to turn it into the upload form for anonymous users:

    $('#sharry').sharry({
      forAlias: 'the-alias'
    });

Also, if the plugins are used in custom pages, make sure the `actionUrl` setting (sometimes called
differently, please see the source code) correctly points to the json script.