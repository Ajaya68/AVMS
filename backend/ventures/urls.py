"""URL configuration for the ventures app."""

from django.urls import path

from . import views

app_name = "ventures"

urlpatterns = [
    path("", views.VentureListCreateView.as_view(), name="venture-list"),
    path("<int:pk>/", views.VentureDetailView.as_view(), name="venture-detail"),
]