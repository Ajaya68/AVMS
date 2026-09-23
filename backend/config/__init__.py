"""AVMS backend package init.

PyMySQL is registered as the MySQLdb driver so that Django's
MySQL backend works without requiring the native mysqlclient build.
"""

import pymysql

pymysql.install_as_MySQLdb()

# Django's MySQL backend requires mysqlclient >= 2.2.1. PyMySQL drives the
# same mysqlclient interface, so report a compatible version to Django.
pymysql.version_info = (2, 2, 1, "final", 0)
pymysql.__version__ = "2.2.1"