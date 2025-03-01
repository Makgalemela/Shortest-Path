import { Injectable } from '@angular/core';
import { FormBuilder, FormGroup } from '@angular/forms';

@Injectable({
  providedIn: 'root'
})
export class SearchFormService {
  constructor(private fb: FormBuilder) { }
  emailForm : FormGroup = this.fb.group({
    origin : '',
    destination : '',
    trafficInfo : false
  })

  resetForm(){
    this.emailForm.patchValue({
      origin : '',
      destination : ''
    })}
}
