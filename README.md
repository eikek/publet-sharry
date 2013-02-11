# Sharry

Sharry is an extension for [publet](https://eknet.org/main/projects/publet/) 
for sharing files with others. Shary aims to be a good mix of easy to use and 
secure. It allows to share files with un-registered users.


## How it works

On a web site you can upload some files together with a password. The site
should be accessed using a secure connection. Sharry receives the files,
creates a zip archive and encrypts it with some symmetric cypher using the
given password. Additionally you can specifiy for how long the link should
be active. After this time, the file is deleted.

On the next step, a form allows you to send the link to this encrypted file to 
some people via email. On each download the password must be given to successfully
decrypt the file. But the link itself is public, everybody can access it, but
only the correct password can decrypt the file.

