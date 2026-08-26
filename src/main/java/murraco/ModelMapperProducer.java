package murraco;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

import org.modelmapper.ModelMapper;

public class ModelMapperProducer {

  @Produces
  @ApplicationScoped
  public ModelMapper modelMapper() {
    return new ModelMapper();
  }
}
