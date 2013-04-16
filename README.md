# Sharry

Sharry is an extension for [publet](https://eknet.org/main/projects/publet/) 
for sharing files with others. Shary aims to be a good mix of easy to use and 
secure. It allows to share files with un-registered users.


## How it works

There are two scenarios: 1) a registered user can share files with anybody that
has an email address. 2) Anybody can share files with registered users (which 
must have a valid email address, too).

For scenario 1): On a web site you can upload some files together with a 
password. The site should be accessed using a secure connection. Sharry receives 
the files, creates a zip archive and encrypts it with some symmetric cypher using 
a given password or creates one itself. Additionally you can specifiy for how 
long the link should be active. After this time, the file is deleted.

On the next step, a form allows you to send the link to this encrypted file to 
some people via email. On each download the password must be given to successfully
decrypt the file. While the page itself is public (everybody can access it), only 
the correct password can decrypt the file.

For scenario 2): A registered user has a personal page, that can be used by
everybody to upload files. Those files are zipped and encrypted like in scenario
1, but the download link is only sent to the registered user this site is for. 
This public page can be reached via a link that is hard to guess, but the page
itself is public again. Registered user can manage the links to this page: create
new ones or disable existing ones (in case you receive spam). 
This public page has some restrictions: you cannot specify a custom password, 
there is a default timeout and a size upload limit.
