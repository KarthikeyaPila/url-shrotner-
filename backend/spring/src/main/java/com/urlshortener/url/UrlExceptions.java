package com.urlshortener.url;

class InvalidUrlException extends RuntimeException { InvalidUrlException() { super("A valid http or https URL is required"); } }
class InvalidAliasException extends RuntimeException { InvalidAliasException() { super("Alias may contain only letters, numbers, - and _"); } }
class AliasAlreadyExistsException extends RuntimeException { AliasAlreadyExistsException(String alias) { super("Alias already exists: " + alias); } }
class UrlNotFoundException extends RuntimeException { UrlNotFoundException(String code) { super("Short URL not found: " + code); } }
